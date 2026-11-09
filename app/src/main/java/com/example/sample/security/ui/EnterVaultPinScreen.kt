package com.example.sample.security.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.example.sample.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EnterVaultPinScreen(
    error: String?,
    failedAttempts: Int,
    onSubmit: (String) -> Unit,
    onForgotPin: () -> Unit,
    onBiometric: (() -> Unit)? = null,
) {
    var pin by remember { mutableStateOf("") }
    val appName = stringResource(R.string.app_name)
    val scheme = MaterialTheme.colorScheme

    Column(
        Modifier.fillMaxSize().background(scheme.background).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(scheme.primary), contentAlignment = Alignment.Center) {
                Text(appName.take(1).uppercase(), color = scheme.onPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(8.dp))
            Text(appName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = scheme.onBackground, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(28.dp))
        Box(Modifier.size(64.dp).clip(CircleShape).background(Color(0xFFE4EDFF)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Lock, null, tint = VaultNavy, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text("Enter Vault PIN", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
        Text(
            "Enter your 6-digit PIN. It is checked on this device only.",
            color = scheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        PinIndicators(pin.length)
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = VaultRed, fontSize = 13.sp)
        }
        TextButton(onClick = onForgotPin) {
            Text("Forgot PIN?", color = scheme.primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(6.dp))
            Text("Reset via security recovery question", color = scheme.onSurfaceVariant, fontSize = 12.sp)
        }
        PinKeypad(
            onDigit = { digit ->
                if (pin.length >= PinLength) return@PinKeypad
                pin += digit
                if (pin.length == PinLength) {
                    onSubmit(pin)
                    pin = ""
                }
            },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            onBiometric = onBiometric,
        )
        Spacer(Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Security, null, tint = scheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Zero-knowledge", color = scheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "PIN hash stays on this phone. Nothing is sent out to unlock the vault.",
            color = scheme.onSurfaceVariant,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Failed attempts: $failedAttempts/5 • 10m lock after 5 misses",
            color = if (failedAttempts >= 5) VaultRed else scheme.onSurfaceVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.height(16.dp))
    }
}
