package com.example.sample.settings

import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sample.R
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.navigation.SetTopBar
import com.example.sample.security.BiometricAvailabilityChecker
import com.example.sample.security.data.AppLockRepository
import com.example.sample.security.data.UnlockMethod
import com.example.sample.security.ui.BiometricUnlockScreen
import com.example.sample.security.ui.PinSetupFlow
import com.example.sample.security.ui.VaultBackground
import com.example.sample.security.ui.VaultRed
import com.example.sample.security.ui.VaultSecondary
import com.example.sample.security.ui.VaultText
import org.koin.compose.koinInject

@Composable
fun UnlockScreen(
    repository: AppLockRepository = koinInject(),
    biometric: BiometricAvailabilityChecker = koinInject(),
) {
    val backStack = LocalBackStack.current
    var method by remember { mutableStateOf(repository.unlockMethod()) }
    var enrolling by remember { mutableStateOf(false) }
    var creatingPin by remember { mutableStateOf(false) }
    var confirmNone by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var methodBeforeEnroll by remember { mutableStateOf(method) }
    val hasPin = repository.isPinSetupComplete()

    if (creatingPin) {
        SetTopBar(isVisible = false)
        PinSetupFlow(
            error = message,
            onDismiss = { creatingPin = false },
        ) { pin, confirm, questionId, answer ->
            message = when {
                pin != confirm -> "PINs do not match"
                !repository.wrapCurrentKeyWithPin(pin, questionId, answer) -> "Could not save the PIN"
                else -> null
            }
            if (message == null) {
                method = UnlockMethod.PIN
                creatingPin = false
            }
        }
        return
    }

    if (enrolling) {
        BiometricUnlockScreen(
            cipher = repository.biometricEnrollCipher(),
            enroll = true,
            allowDeviceCredential = true,
            dismissOnCancel = true,
            onCancel = {
                repository.setUnlockMethod(methodBeforeEnroll)
                method = methodBeforeEnroll
                message = null
                enrolling = false
            },
            onAuthorized = { cipher ->
                if (cipher != null && repository.completeBiometricEnroll(cipher)) {
                    repository.setUnlockMethod(UnlockMethod.DEVICE)
                    method = UnlockMethod.DEVICE
                    message = null
                } else {
                    repository.setUnlockMethod(methodBeforeEnroll)
                    message = "Device lock was not saved."
                }
                enrolling = false
            },
            onError = {
                repository.setUnlockMethod(methodBeforeEnroll)
                method = methodBeforeEnroll
                message = it
                enrolling = false
            },
        )
        return
    }

    SetTopBar(title = "Unlock Method")
    Column(
        Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        UnlockHero()
        Spacer(Modifier.height(18.dp))
        UnlockChoiceCard(
            selected = method == UnlockMethod.DEVICE,
            icon = Icons.Default.PhonelinkLock,
            title = "Use phone lock",
            subtitle = "Fingerprint or device PIN",
            onClick = {
                if (method == UnlockMethod.DEVICE) return@UnlockChoiceCard
                if (!biometric.isAvailable()) {
                    message = "Set a screen lock or fingerprint on this phone first."
                } else {
                    methodBeforeEnroll = method
                    repository.setUnlockMethod(UnlockMethod.DEVICE)
                    enrolling = true
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        UnlockChoiceCard(
            selected = method == UnlockMethod.PIN,
            icon = null,
            title = "Use vault PIN",
            subtitle = "Use your personal vault PIN",
            onClick = {
                if (!hasPin) {
                    creatingPin = true
                } else if (method != UnlockMethod.PIN) {
                    repository.setUnlockMethod(UnlockMethod.PIN)
                    method = UnlockMethod.PIN
                    message = null
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        UnlockChoiceCard(
            selected = method == UnlockMethod.NONE,
            icon = Icons.Default.LockOpen,
            title = "No lock",
            subtitle = "Open without authentication",
            onClick = { if (method != UnlockMethod.NONE) confirmNone = true },
        )
        if (method == UnlockMethod.PIN && hasPin) {
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = Color(0xFFE3E8F0))
            Spacer(Modifier.height(14.dp))
            Text(
                "Vault PIN Settings",
                color = VaultSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(10.dp))
            Surface(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { backStack.add(Destinations.ChangePin) },
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp,
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    UnlockIconTile(icon = Icons.Default.Key, mark = null)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text("Change vault PIN", fontWeight = FontWeight.Bold, color = VaultText, fontSize = 16.sp)
                        Text("Update your existing PIN", color = VaultSecondary, fontSize = 13.sp)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFFB7C0CE))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color(0xFFE8F1FF)) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = Color(0xFF2F6BFF), modifier = Modifier.size(20.dp))
                Text(
                    "Using no lock is less secure. We recommend using a PIN or your device lock.",
                    color = Color(0xFF3D4F73),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = VaultRed, fontSize = 13.sp)
        }
        Spacer(Modifier.height(16.dp))
    }

    if (confirmNone) {
        AlertDialog(
            onDismissRequest = { confirmNone = false },
            title = { Text("Remove the vault lock?") },
            text = { Text("The app will not ask for a PIN, fingerprint, or phone lock. Anyone who opens the app can read the vault.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmNone = false
                    if (repository.enableNoAuthentication()) {
                        method = UnlockMethod.NONE
                        message = null
                    } else {
                        message = "Could not remove the lock."
                    }
                }) { Text("Remove lock") }
            },
            dismissButton = {
                TextButton(onClick = { confirmNone = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun UnlockHero() {
    val shield = ImageBitmap.imageResource(R.drawable.vault_shield)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(Color(0xFFC2D7F6)),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(86.dp)) {
                drawImage(
                    image = shield,
                    dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Choose how you want to unlock your vault.",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = VaultText,
                lineHeight = 26.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Your data stays secure, no matter which option you choose.",
                color = VaultSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun UnlockChoiceCard(
    selected: Boolean,
    icon: ImageVector?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val border = if (selected) Color(0xFF2F6BFF) else Color.Transparent
    val fill = if (selected) Color(0xFFEAF2FF) else Color.White
    Surface(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = fill,
        shadowElevation = if (selected) 0.dp else 1.dp,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            UnlockIconTile(icon = icon, mark = if (icon == null) "123" else null)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.Bold, color = VaultText, fontSize = 16.sp)
                Text(subtitle, color = VaultSecondary, fontSize = 13.sp)
            }
            UnlockRadio(selected)
        }
    }
}

@Composable
private fun UnlockIconTile(icon: ImageVector?, mark: String?) {
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFE7F0FF)),
        contentAlignment = Alignment.Center,
    ) {
        if (mark != null) {
            Text(mark, color = Color(0xFF2F6BFF), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        } else if (icon != null) {
            Icon(icon, null, tint = Color(0xFF2F6BFF), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun UnlockRadio(selected: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .border(2.dp, if (selected) Color(0xFF2F6BFF) else Color(0xFFC5CAD6), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF2F6BFF)))
        }
    }
}
