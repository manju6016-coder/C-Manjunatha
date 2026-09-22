package com.example.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executor

/**
 * Status of biometric capability on the device.
 */
sealed class BiometricCapability {
    object Available : BiometricCapability()
    data class Unavailable(val reason: String) : BiometricCapability()
}

/**
 * Result callback for biometric prompt authentication.
 */
sealed class BiometricAuthResult {
    object Success : BiometricAuthResult()
    data class Error(val errorCode: Int, val errString: CharSequence) : BiometricAuthResult()
    object Failed : BiometricAuthResult()
    object Canceled : BiometricAuthResult()
}

/**
 * Helper utility for managing biometric checks and launching BiometricPrompt.
 */
object BiometricAuthManager {

    /**
     * Checks if the device has biometric hardware and enrolled biometrics (or device credential fallback).
     */
    fun checkBiometricCapability(context: Context): BiometricCapability {
        val biometricManager = BiometricManager.from(context)
        val authenticators = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricCapability.Available
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricCapability.Unavailable("No biometric hardware (fingerprint/face) detected on this device.")
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricCapability.Unavailable("Biometric hardware is currently unavailable. Try device PIN/password.")
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricCapability.Unavailable("No fingerprint or face enrolled. Please set up lock screen security in device settings.")
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                BiometricCapability.Unavailable("Security update required to use biometric authentication.")
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                BiometricCapability.Unavailable("Biometric authentication is unsupported.")
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                BiometricCapability.Unavailable("Biometric status is unknown.")
            }
            else -> {
                BiometricCapability.Unavailable("Biometrics not available.")
            }
        }
    }

    /**
     * Checks if biometric hardware is present (even if not enrolled).
     */
    fun hasBiometricHardware(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val res = biometricManager.canAuthenticate(BIOMETRIC_STRONG)
        return res != BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
    }

    /**
     * Launches the AndroidX BiometricPrompt on a FragmentActivity.
     */
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "Admin Biometric Security",
        subtitle: String = "Fingerprint or Face Authentication",
        description: String = "Authenticate your identity before accessing sensitive financial ledger records.",
        onAuthResult: (BiometricAuthResult) -> Unit
    ) {
        val executor: Executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthResult(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onAuthResult(BiometricAuthResult.Canceled)
                } else {
                    onAuthResult(BiometricAuthResult.Error(errorCode, errString))
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onAuthResult(BiometricAuthResult.Failed)
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        biometricPrompt.authenticate(promptInfo)
    }
}
