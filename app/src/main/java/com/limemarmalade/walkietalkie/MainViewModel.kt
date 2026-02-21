package com.limemarmalade.walkietalkie

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.limemarmalade.walkietalkie.billing.BillingManager
import com.limemarmalade.walkietalkie.licensing.LicenseChecker
import java.net.NetworkInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class Screen {
    Selection,
    Host,
    Client,
    Connected,
    PurchaseRequired
}

enum class Mode {
    HOST,
    CLIENT
}

class MainViewModel(
    application: Application,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : AndroidViewModel(application) {

    private val _screen = MutableStateFlow(Screen.Selection)
    val screen = _screen.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium = _isPremium.asStateFlow()

    private val _purchaseError = MutableStateFlow<String?>(null)
    val purchaseError = _purchaseError.asStateFlow()

    private val _showPrivacyPolicy = MutableStateFlow(false)
    val showPrivacyPolicy = _showPrivacyPolicy.asStateFlow()

    private val _showTermsOfService = MutableStateFlow(false)
    val showTermsOfService = _showTermsOfService.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog = _showSettingsDialog.asStateFlow()

    private val _showPurchaseSuccess = MutableStateFlow(false)
    val showPurchaseSuccess = _showPurchaseSuccess.asStateFlow()

    private val _productPrice = MutableStateFlow<String?>(null)
    val productPrice = _productPrice.asStateFlow()

    private val prefs = application.getSharedPreferences("walkie_talkie_prefs", Context.MODE_PRIVATE)

    private val _ipAddress = MutableStateFlow(getLastIpAddress())
    val ipAddress = _ipAddress.asStateFlow()

    private var billingManager: BillingManager? = null
    private var licenseChecker: LicenseChecker? = null

    private val _clientCount = MutableStateFlow(0)
    val clientCount = _clientCount.asStateFlow()

    fun onModeSelected(mode: Mode) {
        if (!isPremium.value) {
            _screen.value = Screen.PurchaseRequired
            return
        }
        
        _screen.value = when (mode) {
            Mode.HOST -> {
                viewModelScope.launch(dispatcher) {
                    _ipAddress.value = getIpAddress()
                    val intent = Intent(getApplication(), WalkieTalkieService::class.java).apply {
                        action = "HOST"
                    }
                    getApplication<Application>().startService(intent)
                }
                Screen.Host
            }
            Mode.CLIENT -> Screen.Client
        }
    }

    fun onConnect(ip: String) {
        if (!isPremium.value) {
            _screen.value = Screen.PurchaseRequired
            return
        }
        
        saveIpAddress(ip)
        val intent = Intent(getApplication(), WalkieTalkieService::class.java).apply {
            action = "CLIENT"
            putExtra("IP", ip)
        }
        getApplication<Application>().startService(intent)
        _screen.value = Screen.Connected
    }

    fun updateClientCount(count: Int) {
        _clientCount.value = count
    }

    private fun saveIpAddress(ip: String) {
        val editor = prefs.edit()
        editor.putString("last_ip", ip)
        editor.apply()
    }

    private fun getLastIpAddress(): String {
        return prefs.getString("last_ip", "") ?: ""
    }

    private fun getIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                if (networkInterface.isUp && !networkInterface.isLoopback &&
                    (networkInterface.displayName.contains("wlan", ignoreCase = true) ||
                            networkInterface.displayName.contains("ap", ignoreCase = true))
                ) {
                    val addresses = networkInterface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val inetAddress = addresses.nextElement()
                        if (!inetAddress.isLoopbackAddress && inetAddress.hostAddress.contains(".")) {
                            return inetAddress.hostAddress
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Log the exception or handle it as needed
        }
        return ""
    }

    fun initializeBilling(context: Context) {
        billingManager = BillingManager(context) { isSuccess, errorMessage ->
            if (isSuccess) {
                setPremiumStatus(true)
                prefs.edit().putBoolean("is_premium", true).apply()
                showPurchaseSuccess()
            } else {
                _purchaseError.value = errorMessage ?: "Purchase failed"
            }
        }
        billingManager?.startConnection()
        
        // Initialize licensing checker
        // Note: In production, you would get this from your secure backend or build config
        // For demo purposes, we'll use a placeholder signature
        val expectedAppSignature = "YOUR_EXPECTED_APP_SIGNATURE_HASH"
        try {
            licenseChecker = LicenseChecker(context, expectedAppSignature) { isValid, errorMessage ->
                if (!isValid) {
                    Log.w("Licensing", "License check failed: ${errorMessage ?: "Unknown error"}")
                    // For strict licensing, you might want to disable functionality here
                    // For this demo, we'll allow the app to continue but log the issue
                }
            }
        } catch (e: Exception) {
            Log.e("Licensing", "Failed to initialize license checker: ${e.message}")
            // Continue without licensing for demo purposes
        }
        
        // Check if user is already premium
        val isPremiumUser = prefs.getBoolean("is_premium", false)
        if (isPremiumUser) {
            setPremiumStatus(true)
            // Perform license check for premium users
            licenseChecker?.checkLicense()
        }
    }

    fun startPurchase(activity: Activity) {
        billingManager?.launchPurchaseFlow(activity)
    }

    fun restorePurchases() {
        billingManager?.restorePurchases()
    }

    fun updateProductPrice(price: String?) {
        _productPrice.value = price
    }

    fun setPremiumStatus(isPremium: Boolean) {
        _isPremium.value = isPremium
        if (isPremium) {
            _screen.value = Screen.Selection
        }
    }

    fun getCurrentProductPrice(): String? {
        return billingManager?.getProductPrice()
    }

    fun showPrivacyPolicy() {
        _showPrivacyPolicy.value = true
    }

    fun hidePrivacyPolicy() {
        _showPrivacyPolicy.value = false
    }

    fun showSettingsDialog() {
        _showSettingsDialog.value = true
    }

    fun hideSettingsDialog() {
        _showSettingsDialog.value = false
    }

    fun showPurchaseSuccess() {
        _showPurchaseSuccess.value = true
    }

    fun hidePurchaseSuccess() {
        _showPurchaseSuccess.value = false
    }

    fun showTermsOfService() {
        _showTermsOfService.value = true
    }

    fun hideTermsOfService() {
        _showTermsOfService.value = false
    }

    fun clearError() {
        _purchaseError.value = null
    }

    public override fun onCleared() {
        super.onCleared()
        billingManager?.endConnection()
        licenseChecker?.onDestroy()
        getApplication<Application>().stopService(Intent(getApplication(), WalkieTalkieService::class.java))
    }
}