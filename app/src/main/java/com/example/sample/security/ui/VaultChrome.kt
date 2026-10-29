package com.example.sample.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal const val PinLength = 6

internal val VaultNavy = Color(0xFF102F63)
internal val VaultBackground = Color(0xFFF5F7FA)
internal val VaultCard = Color(0xFFFFFFFF)
internal val VaultText = Color(0xFF20242B)
internal val VaultSecondary = Color(0xFF536783)
internal val VaultGreen = Color(0xFF8CEBD6)
internal val VaultGreenText = Color(0xFF064D42)
internal val VaultRed = Color(0xFFB42318)
internal val VaultRedBackground = Color(0xFFFFE0DD)

@Composable
internal fun VaultHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable () -> Unit = { LockBadge() }) {
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = VaultText)
            }
        } else {
            Spacer(Modifier.width(48.dp))
        }
        Text(
            title,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = VaultText,
        )
        trailing()
    }
}

@Composable
internal fun LockBadge() {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(VaultNavy),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Default.Lock, "Vault security", tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
internal fun SecurityHero(title: String, description: String, icon: ImageVector) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(Color(0xFFDCE6FF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = VaultNavy, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(12.dp))
        Text(title, fontSize = 26.sp, fontWeight = FontWeight.Bold, color = VaultText, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(description, fontSize = 15.sp, lineHeight = 22.sp, color = VaultSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
internal fun PinIndicators(length: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        repeat(PinLength) { index ->
            Box(
                Modifier.size(13.dp).clip(CircleShape).background(
                    if (index < length) VaultNavy else Color(0xFFE3E6E9),
                ),
            )
        }
    }
}

@Composable
internal fun VaultSurface(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = VaultCard, shadowElevation = 1.dp) {
        Column(Modifier.padding(18.dp), content = content)
    }
}

@Composable
internal fun ChecklistItem(text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).clip(CircleShape).background(VaultGreen), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CheckCircle, null, tint = VaultGreenText, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontSize = 14.sp, color = Color(0xFF4A4F59))
    }
}

@Composable
internal fun VaultPrimaryButton(text: String, icon: ImageVector = Icons.Default.Lock, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = VaultNavy, contentColor = Color.White),
    ) {
        Icon(icon, null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
