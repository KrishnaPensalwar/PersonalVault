package com.example.sample.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class UnlockChoice { DEVICE, PIN, NONE }

@Composable
fun ChooseUnlockScreen(
    error: String?,
    onDevice: () -> Unit,
    onPin: () -> Unit,
    onNone: () -> Unit,
) {
    var choice by rememberSaveable { mutableStateOf(UnlockChoice.DEVICE) }
    Column(
        Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Surface(shape = CircleShape, color = Color(0xFFE7EEF9)) {
            Text(
                "STEP 1 OF 2",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = VaultNavy,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(14.dp))
        Text("How should this vault open?", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14315C), lineHeight = 34.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "You can change this later in Settings.",
            color = VaultSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(16.dp))
        ChoiceCard(
            selected = choice == UnlockChoice.DEVICE,
            icon = Icons.Default.Fingerprint,
            title = "Use this phone's lock",
            body = "Fingerprint or the phone PIN unwraps the vault key.",
            badge = "Recommended",
            onClick = { choice = UnlockChoice.DEVICE },
        )
        Spacer(Modifier.height(10.dp))
        ChoiceCard(
            selected = choice == UnlockChoice.PIN,
            icon = Icons.Default.GridView,
            title = "Create a custom vault PIN",
            body = "A 6-digit PIN and a recovery passphrase, separate from the phone lock.",
            badge = null,
            onClick = { choice = UnlockChoice.PIN },
        )
        Spacer(Modifier.height(10.dp))
        ChoiceCard(
            selected = choice == UnlockChoice.NONE,
            icon = Icons.Default.LockOpen,
            title = "No lock",
            body = "The vault opens without a fingerprint, phone PIN, or vault PIN.",
            badge = null,
            onClick = { choice = UnlockChoice.NONE },
        )
        if (choice == UnlockChoice.NONE) {
            Spacer(Modifier.height(10.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFFFFE8E6)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = VaultRed)
                    Text(
                        "Anyone who can open the app can read the vault. Do not use this on a shared phone.",
                        color = VaultRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = VaultRed, fontSize = 13.sp)
        }
        Spacer(Modifier.height(18.dp))
        VaultPrimaryButton("Continue", Icons.AutoMirrored.Filled.ArrowForward) {
            when (choice) {
                UnlockChoice.DEVICE -> onDevice()
                UnlockChoice.PIN -> onPin()
                UnlockChoice.NONE -> onNone()
            }
        }
    }
}

@Composable
private fun ChoiceCard(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    body: String,
    badge: String?,
    onClick: () -> Unit,
) {
    val border = if (selected) VaultNavy else Color(0xFFE3E8F0)
    Surface(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.5.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
    ) {
        Row(Modifier.padding(14.dp)) {
            Icon(icon, null, tint = VaultNavy, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, color = VaultText, modifier = Modifier.weight(1f))
                }
                if (badge != null) {
                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFE7F6EF)) {
                        Text(badge, color = Color(0xFF067647), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                Text(body, color = VaultSecondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp))
            }
            BoxRadio(selected)
        }
    }
}

@Composable
private fun BoxRadio(selected: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .border(2.dp, if (selected) VaultNavy else Color(0xFFC5CAD6), CircleShape)
            .background(if (selected) VaultNavy else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color.White))
        }
    }
}
