package com.example.sample.security.ui

import android.os.Build
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

@Composable
fun BiometricUnlockScreen(
    cipher: Cipher?,
    enroll: Boolean,
    onAuthorized: (Cipher?) -> Unit,
    onError: (String) -> Unit,
) {
    val activity = LocalContext.current as FragmentActivity
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTrigger, cipher) {
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
                onError(errString.toString())
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (enroll) "Protect vault key" else "Unlock vault")
            .setSubtitle(if (enroll) "Biometric unlock wraps the vault key" else "Authentication unwraps the vault key")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
        } else {
            builder.setAllowedAuthenticators(BIOMETRIC_STRONG)
            builder.setNegativeButtonText("Use PIN")
        }
        prompt.authenticate(builder.build(), BiometricPrompt.CryptoObject(cipher))
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (enroll) "Confirm biometric protection…" else "Waiting for authentication…")
        Button(onClick = { retryTrigger++ }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Try Again")
        }
    }
}
