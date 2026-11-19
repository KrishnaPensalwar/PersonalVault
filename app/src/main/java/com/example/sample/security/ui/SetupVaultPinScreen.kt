package com.example.sample.security.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.vaulet.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PinSetupFlow(
    error: String?,
    saving: Boolean = false,
    onDismiss: (() -> Unit)? = null,
    onSubmit: (pin: String, confirmPin: String, recoveryQuestionId: Int, recoveryAnswer: String) -> Unit,
) {
    var step by remember { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    when (step) {
        0 -> SetupVaultPinScreen(
            confirming = false,
            error = localError ?: error,
            onBack = onDismiss,
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
            saving = saving,
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
    var pending by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(pending) {
        val value = pending ?: return@LaunchedEffect
        withFrameNanos { }
        onPinReady(value)
        pending = null
        pin = ""
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF7FBFF), Color(0xFFEAF2FF)))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VaultHeader(if (confirming) "Confirm Vault PIN" else "Setup Vault PIN", onBack)
            Image(
                painterResource(R.drawable.ic_vault_shield),
                contentDescription = null,
                modifier = Modifier.size(132.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                if (confirming) "Confirm Vault PIN" else "Create Vault PIN",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0B1F44),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (confirming) {
                    "Re-enter the same 6-digit PIN to confirm it."
                } else {
                    "Set a 6-digit PIN to quickly unlock your vault and decrypt credentials on this device."
                },
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = Color(0xFF5C6578),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            PinDots(pin.length)
            Spacer(Modifier.height(10.dp))
            Text(
                "${pin.length} of $PinLength digits entered",
                color = Color(0xFF4B5568),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
            if (!confirming) {
                Spacer(Modifier.height(16.dp))
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Hardware Security Protocol", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0B1F44))
                        Spacer(Modifier.height(8.dp))
                        ProtocolLine("6 digits required for key derivation")
                        ProtocolLine("Avoid simple repetitions (e.g. 111111)")
                        ProtocolLine("Hardware-backed Keystore AES-256")
                    }
                }
            } else {
                Spacer(Modifier.height(28.dp))
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
                        pending = pin
                    }
                },
                onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            )
            Spacer(Modifier.height(14.dp))
            Surface(shape = RoundedCornerShape(50), color = Color(0xFFE7F0FF)) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Info, null, tint = Color(0xFF1D4ED8), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Both entries must match before recovery setup.",
                        color = Color(0xFF1D4ED8),
                        fontSize = 12.sp,
                    )
                }
            }
        }
        if (pending != null) {
            Box(Modifier.fillMaxSize().background(Color(0xCCF7FBFF)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF1D4ED8))
            }
        }
    }
}

@Composable
private fun PinDots(length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(PinLength) { index ->
            Box(
                Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(if (index < length) Color(0xFF1D4ED8) else Color(0xFFD5DCE8)),
            )
        }
    }
}

@Composable
private fun ProtocolLine(text: String) {
    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF12B76A), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF3D4658))
    }
}
