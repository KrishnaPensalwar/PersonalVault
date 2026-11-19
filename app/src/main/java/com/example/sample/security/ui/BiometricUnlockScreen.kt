package com.example.sample.security.ui

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

@Composable
fun BiometricUnlockScreen(
    cipher: Cipher?,
    enroll: Boolean,
    allowDeviceCredential: Boolean,
    dismissOnCancel: Boolean = false,
    onAuthorized: (Cipher?) -> Unit,
    onCancel: () -> Unit = {},
    onError: (String) -> Unit,
) {
    val activity = LocalContext.current as FragmentActivity
    var retryTrigger by remember { mutableIntStateOf(0) }
    var promptOpen by remember { mutableStateOf(true) }

    LaunchedEffect(retryTrigger, cipher) {
        promptOpen = true
        if (cipher == null) {
            onError("Vault key is not ready for biometric unlock")
            return@LaunchedEffect
        }
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onAuthorized(result.cryptoObject?.cipher)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val cancelled = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                if (cancelled) {
                    promptOpen = false
                    if (dismissOnCancel) onCancel()
                } else {
                    onError(errString.toString())
                }
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (enroll) "Protect vault key" else "Unlock vault")
            .setSubtitle(if (enroll) "Biometric unlock wraps the vault key" else "Authentication unwraps the vault key")
        if (allowDeviceCredential && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            builder.setNegativeButtonText(if (allowDeviceCredential) "Cancel" else "Use vault PIN")
        }
        prompt.authenticate(builder.build(), BiometricPrompt.CryptoObject(cipher))
    }

    Box(Modifier.fillMaxSize().background(VaultBackground), contentAlignment = Alignment.Center) {
        if (!promptOpen && !dismissOnCancel) {
            Button(onClick = { retryTrigger++ }) { Text("Unlock") }
        }
    }
}
