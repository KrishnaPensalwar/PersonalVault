package com.example.sample.security.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun SetNewPinScreen(
    error: String?,
    onSubmit: (pin: String, confirmPin: String) -> Unit,
) {
    var created by remember { mutableStateOf<String?>(null) }
    var localError by remember { mutableStateOf<String?>(null) }
    val confirming = created != null

    SetupVaultPinScreen(
        confirming = confirming,
        error = localError ?: error,
        onBack = if (confirming) {
            { created = null; localError = null }
        } else {
            null
        },
        onPinReady = { value ->
            val first = created
            if (first == null) {
                created = value
            } else if (value != first) {
                localError = "PINs do not match."
            } else {
                onSubmit(first, value)
            }
        },
    )
}
