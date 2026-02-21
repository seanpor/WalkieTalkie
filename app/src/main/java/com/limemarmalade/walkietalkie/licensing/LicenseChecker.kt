package com.limemarmalade.walkietalkie.licensing

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Base64
import android.util.Log
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Simple license checker that verifies app signature and purchase status
 * This provides basic anti-piracy protection without requiring the full LVL library
 */
class LicenseChecker(
    private val context: Context,
    private val expectedSignature: String, // Expected app signature hash
    private val onLicenseCheckResult: (Boolean, String?) -> Unit
) {
    
    fun checkLicense() {
        try {
            // 1. Check app signature
            val isSignatureValid = checkAppSignature()
            if (!isSignatureValid) {
                onLicenseCheckResult(false, "Invalid app signature")
                return
            }
            
            // 2. For paid apps, we would also check purchase status here
            // Since we're using in-app billing, the purchase verification is handled by BillingManager
            
            Log.d("Licensing", "License check passed")
            onLicenseCheckResult(true, null)
            
        } catch (e: Exception) {
            Log.e("Licensing", "License check failed: ${e.message}")
            onLicenseCheckResult(false, "License verification failed")
        }
    }

    private fun checkAppSignature(): Boolean {
        try {
            // Use modern API if available, fall back to deprecated one
            val packageInfo: PackageInfo = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES
                    )
                } else {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(
                        context.packageName,
                        PackageManager.GET_SIGNATURES
                    )
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("Licensing", "Package not found: ${e.message}")
                return false
            }
            
            // Get signatures based on API level
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                @Suppress("DEPRECATION")
                packageInfo.signingInfo?.apkContentsSigners ?: packageInfo.signingInfo?.signingCertificateHistory
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }
            
            if (signatures == null || signatures.isEmpty()) {
                Log.e("Licensing", "No signatures found")
                return false
            }
            
            // Get the first signature and calculate its hash
            val signature: Signature = signatures[0]
            val signatureBytes = getSignatureBytes(signature)
            val signatureHash = getSignatureHash(signatureBytes)
            
            Log.d("Licensing", "Current signature: $signatureHash")
            Log.d("Licensing", "Expected signature: $expectedSignature")
            
            // Compare with expected signature
            return signatureHash.equals(expectedSignature, ignoreCase = true)
            
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("Licensing", "Package not found: ${e.message}")
            return false
        } catch (e: Exception) {
            Log.e("Licensing", "Signature check failed: ${e.message}")
            return false
        }
    }

    private fun getSignatureHash(signatureBytes: ByteArray): String {
        try {
            val md: MessageDigest = MessageDigest.getInstance("SHA-256")
            md.update(signatureBytes)
            val digest: ByteArray = md.digest()
            return bytesToHex(digest)
        } catch (e: NoSuchAlgorithmException) {
            Log.e("Licensing", "SHA-256 algorithm not available")
            return ""
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexArray = "0123456789ABCDEF".toCharArray()
        val hexChars = CharArray(bytes.size * 2)
        for (i in bytes.indices) {
            val v = bytes[i].toInt() and 0xFF
            hexChars[i * 2] = hexArray[v ushr 4]
            hexChars[i * 2 + 1] = hexArray[v and 0x0F]
        }
        return String(hexChars)
    }

    fun onDestroy() {
        // Cleanup if needed
    }

    // Helper function to get signature bytes
    private fun getSignatureBytes(signature: Signature): ByteArray {
        return signature.getCharsString().toByteArray(Charsets.UTF_8)
    }
    
    private fun Signature.getCharsString(): String {
        return Base64.encodeToString(this.toByteArray(), Base64.NO_WRAP)
    }
}