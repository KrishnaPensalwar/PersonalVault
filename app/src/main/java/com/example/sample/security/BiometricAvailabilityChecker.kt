package com.example.sample.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL

/**
 * Reports whether the device already has biometric enrollment OR a device
 * credential (PIN/pattern/password) set up — in either case we hand off to
 * [androidx.biometric.BiometricPrompt] instead of the app's own PIN flow.
 */
interface BiometricAvailabilityChecker {
    fun isAvailable(): Boolean
}

class BiometricAvailabilityCheckerImpl(
    private val context: Context,
) : BiometricAvailabilityChecker {

    override fun isAvailable(): Boolean {
        val result = BiometricManager.from(context)
            .canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        return result == BiometricManager.BIOMETRIC_SUCCESS
    }
}
