package com.limemarmalade.walkietalkie.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BillingManager(
    private val context: Context,
    private val productId: String = "full_version_unlock",
    private val onPurchaseComplete: (Boolean, String?) -> Unit
) {
    private val billingClient = BillingClient.newBuilder(context)
        .setListener { billingResult, purchases ->
            handlePurchases(billingResult, purchases)
        }
        .enablePendingPurchases()
        .build()

    private var currentProductDetails: ProductDetails? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun startConnection() {
        if (billingClient.isReady) {
            Log.d("Billing", "Billing client already connected")
            queryPurchases()
            queryProductDetails()
            return
        }

        try {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    if (billingResult.responseCode == BillingResponseCode.OK) {
                        Log.d("Billing", "Billing client connected successfully")
                        queryPurchases()
                        queryProductDetails()
                    } else {
                        Log.e("Billing", "Billing setup failed: ${billingResult.responseCode} - ${billingResult.debugMessage}")
                        handleBillingError(billingResult.responseCode, "Billing service unavailable")
                    }
                }

                override fun onBillingServiceDisconnected() {
                    Log.w("Billing", "Billing service disconnected")
                    // Try to reconnect with exponential backoff
                    reconnectWithBackoff(1) // Start with 1st attempt
                }
            })
        } catch (e: Exception) {
            Log.e("Billing", "Failed to start billing connection: ${e.message}")
            onPurchaseComplete(false, "Billing service error")
        }
    }

    private var reconnectAttempt = 0
    private val maxReconnectAttempts = 3

    private fun reconnectWithBackoff(attempt: Int) {
        if (attempt > maxReconnectAttempts) {
            Log.e("Billing", "Max reconnection attempts reached")
            return
        }

        val delayMs = when (attempt) {
            1 -> 2000
            2 -> 5000
            3 -> 10000
            else -> 5000
        }

        coroutineScope.launch {
            try {
                Log.w("Billing", "Attempting to reconnect (attempt $attempt) in ${delayMs}ms...")
                delay(delayMs.toLong())
                if (!billingClient.isReady) {
                    startConnection()
                }
            } catch (e: Exception) {
                Log.e("Billing", "Reconnection failed: ${e.message}")
            }
        }
    }

    private fun handleBillingError(errorCode: Int, defaultMessage: String) {
        val errorMessage = when (errorCode) {
            BillingResponseCode.SERVICE_TIMEOUT -> "Billing service timeout"
            BillingResponseCode.FEATURE_NOT_SUPPORTED -> "Billing not supported on this device"
            BillingResponseCode.SERVICE_DISCONNECTED -> "Billing service disconnected"
            BillingResponseCode.SERVICE_UNAVAILABLE -> "Billing service unavailable"
            BillingResponseCode.DEVELOPER_ERROR -> "Developer configuration error"
            BillingResponseCode.ERROR -> "Billing error occurred"
            BillingResponseCode.USER_CANCELED -> "Purchase canceled by user"
            BillingResponseCode.ITEM_ALREADY_OWNED -> "Item already owned"
            BillingResponseCode.ITEM_NOT_OWNED -> "Item not owned"
            else -> defaultMessage
        }
        
        Log.e("Billing", "Billing error $errorCode: $errorMessage")
        onPurchaseComplete(false, errorMessage)
    }

    private fun queryProductDetails() {
        if (!billingClient.isReady) return

        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(productId)
                .setProductType(ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                currentProductDetails = productDetailsList.firstOrNull()
                Log.d("Billing", "Product details loaded: ${currentProductDetails?.name}")
                
                // Get the localized price
                currentProductDetails?.let { details ->
                    val price = details.oneTimePurchaseOfferDetails?.formattedPrice
                    Log.d("Billing", "Product price: $price")
                }
            } else {
                Log.e("Billing", "Failed to query product details: ${billingResult.debugMessage}")
            }
        }
    }

    private fun queryPurchases() {
        if (!billingClient.isReady) return

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingResponseCode.OK && purchases.isNotEmpty()) {
                Log.d("Billing", "Found ${purchases.size} existing purchases")
                handlePurchases(billingResult, purchases)
            }
        }
    }

    private fun handlePurchases(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode != BillingResponseCode.OK || purchases.isNullOrEmpty()) {
            return
        }

        for (purchase in purchases) {
            when (purchase.purchaseState) {
                Purchase.PurchaseState.PURCHASED -> {
                    if (!purchase.isAcknowledged) {
                        acknowledgePurchase(purchase)
                    }
                    verifyPurchase(purchase)
                }
                Purchase.PurchaseState.PENDING -> {
                    Log.w("Billing", "Purchase is pending: ${purchase.orderId}")
                }
                Purchase.PurchaseState.UNSPECIFIED_STATE -> {
                    Log.w("Billing", "Purchase in unspecified state: ${purchase.orderId}")
                }
            }
        }
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        if (purchase.isAcknowledged) return

        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { billingResult ->
            if (billingResult.responseCode == BillingResponseCode.OK) {
                Log.d("Billing", "Purchase acknowledged: ${purchase.orderId}")
            } else {
                Log.e("Billing", "Failed to acknowledge purchase: ${billingResult.debugMessage}")
            }
        }
    }

    private fun verifyPurchase(purchase: Purchase) {
        // In a production app, you would verify the purchase with your backend server
        // For this demo, we'll do a basic client-side verification
        if (purchase.products.contains(productId)) {
            Log.d("Billing", "Purchase verified successfully")
            onPurchaseComplete(true, null)
        } else {
            Log.e("Billing", "Purchase verification failed: product ID mismatch")
            onPurchaseComplete(false, "Purchase verification failed")
        }
    }

    fun launchPurchaseFlow(activity: Activity) {
        if (!billingClient.isReady) {
            onPurchaseComplete(false, "Billing service not ready")
            return
        }

        currentProductDetails?.let { productDetails ->
            val offerDetails = productDetails.oneTimePurchaseOfferDetails
                ?: run {
                    onPurchaseComplete(false, "No purchase offer available")
                    return
                }

            // For BillingClient 6.0+, we don't need to explicitly set offerToken
            // The ProductDetailsParams.builder() handles it automatically
            val billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .build()
                    )
                )
                .build()

            val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
            if (billingResult.responseCode != BillingResponseCode.OK) {
                Log.e("Billing", "Failed to launch billing flow: ${billingResult.debugMessage}")
                onPurchaseComplete(false, "Purchase failed: ${billingResult.debugMessage}")
            }
        } ?: run {
            onPurchaseComplete(false, "Product details not loaded")
        }
    }

    fun getProductPrice(): String? {
        return currentProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            onPurchaseComplete(false, "Billing client not ready")
            return
        }
        
        Log.d("Billing", "Restoring purchases...")
        queryPurchases()
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun isReady(): Boolean = billingClient.isReady
}