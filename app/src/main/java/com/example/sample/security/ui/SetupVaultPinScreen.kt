package com.example.sample.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinSetupFlow(
    error: String?,
    onSubmit: (pin: String, confirmPin: String, recoveryQuestionId: Int, recoveryAnswer: String) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    when (step) {
        0 -> SetupVaultPinScreen(
            confirming = false,
            error = localError ?: error,
            onBack = null,
            onPinReady = {
                pin = it
                localError = null
                step = 1
            },
        )
        1 -> SetupVaultPinScreen(
            confirming = true,
            error = localError,
            onBack = { step = 0; localError = null },
            onPinReady = { confirm ->
                if (confirm != pin) {
                    localError = "PINs do not match."
                } else {
                    localError = null
                    step = 2
                }
            },
        )
        else -> SetupRecoveryQuestionScreen(
            error = localError ?: error,
            onBack = { step = 1 },
            onComplete = { questionId, answer -> onSubmit(pin, pin, questionId, answer) },
        )
    }
}

@Composable
fun SetupVaultPinScreen(
    confirming: Boolean,
    error: String?,
    onBack: (() -> Unit)?,
    onPinReady: (String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var rejected by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(VaultBackground)) {
        VaultHeader(if (confirming) "Confirm Vault PIN" else "Setup Vault PIN", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SecurityHero(
                title = if (confirming) "Confirm Vault PIN" else "Create Vault PIN",
                description = if (confirming) {
                    "Re-enter the same 6-digit PIN to confirm it."
                } else {
                    "Set a 6-digit PIN to quickly unlock your vault and decrypt credentials on this device."
                },
                icon = Icons.Default.Lock,
            )
            Spacer(Modifier.height(8.dp))
            PinIndicators(pin.length)
            Spacer(Modifier.height(8.dp))
            Text(
                "${pin.length} of $PinLength digits entered",
                color = VaultSecondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            if (!confirming) {
                Spacer(Modifier.height(16.dp))
                VaultSurface {
                    Text("Hardware Security Protocol", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.height(8.dp))
                    ChecklistItem("6 digits required for key derivation")
                    ChecklistItem("Avoid simple repetitions (e.g. 111111)")
                    ChecklistItem("Hardware-backed Keystore AES-256")
                }
            }
            (rejected ?: error)?.let {
                Spacer(Modifier.height(10.dp))
                Text(it, color = VaultRed, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))
            PinKeypad(
                onDigit = { digit ->
                    if (pin.length >= PinLength) return@PinKeypad
                    pin += digit
                    rejected = null
                    if (pin.length == PinLength) {
                        if (pin.all { it == pin.first() }) {
                            rejected = "Avoid using six identical digits."
                            pin = ""
                            return@PinKeypad
                        }
                        onPinReady(pin)
                        pin = ""
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            )
            Spacer(Modifier.height(12.dp))
            Text(
                if (confirming) "Both entries must match before recovery setup."
                else "You will be prompted to confirm this PIN on the next step.",
                color = VaultSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
