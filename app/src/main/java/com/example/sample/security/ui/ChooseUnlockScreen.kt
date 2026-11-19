package com.example.sample.security.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Brush
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
import com.vaulet.app.R

private enum class UnlockChoice { DEVICE, PIN, NONE }

private val ChoiceBlue = Color(0xFF1D4ED8)
private val ChoiceTitle = Color(0xFF0B1F44)

@Composable
fun ChooseUnlockScreen(
    error: String?,
    onDevice: () -> Unit,
    onPin: () -> Unit,
    onNone: () -> Unit,
) {
    var choice by rememberSaveable { mutableStateOf(UnlockChoice.DEVICE) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF7FBFF), Color(0xFFEAF2FF)))),
    ) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Surface(shape = RoundedCornerShape(50), color = Color(0xFFE7F0FF)) {
            Text(
                "STEP 1 OF 2",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = ChoiceBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.Top) {
            Text(
                "How should this vault open?",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = ChoiceTitle,
                lineHeight = 36.sp,
                modifier = Modifier.weight(1f),
            )
            Image(
                painterResource(R.drawable.ic_vault_shield),
                contentDescription = null,
                modifier = Modifier.size(108.dp),
                contentScale = ContentScale.Fit,
            )
        }
        Text(
            "You can change this later in Settings.",
            color = Color(0xFF6B7280),
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .width(72.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(50))
                .background(ChoiceBlue),
        )
        Spacer(Modifier.height(18.dp))
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
            icon = Icons.Default.Lock,
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
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = {
                when (choice) {
                    UnlockChoice.DEVICE -> onDevice()
                    UnlockChoice.PIN -> onPin()
                    UnlockChoice.NONE -> onNone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ChoiceBlue, contentColor = Color.White),
        ) {
            Text("Continue", fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
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
    val border = if (selected) ChoiceBlue else Color.Transparent
    Surface(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(if (selected) 1.5.dp else 0.dp, border, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F6FB)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = ChoiceBlue, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, color = ChoiceTitle, fontSize = 15.sp)
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFE5F8EC)) {
                            Row(
                                Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("✓ ", color = Color(0xFF128A3E), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(badge, color = Color(0xFF128A3E), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
                Text(body, color = Color(0xFF6B7280), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 2.dp))
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
            .border(2.dp, if (selected) ChoiceBlue else Color(0xFFD0D5DD), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(ChoiceBlue))
        }
    }
}
