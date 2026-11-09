package com.example.sample.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NoEncryption
import androidx.compose.material.icons.filled.PhonelinkLock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            onAuthorized = { cipher ->
                if (cipher != null && repository.completeBiometricEnroll(cipher)) {
                    repository.setUnlockMethod(UnlockMethod.DEVICE)
                    method = UnlockMethod.DEVICE
                    message = null
                } else {
                    message = "Device lock was not saved."
                }
                enrolling = false
            },
            onError = {
                message = it
                enrolling = false
            },
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(VaultBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        SectionLabel("CURRENT")
        Text(unlockStatus(method), modifier = Modifier.padding(start = 4.dp, bottom = 18.dp), fontSize = 16.sp)
        SectionLabel("CHANGE")
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column {
                SettingsRow(
                    icon = Icons.Default.PhonelinkLock,
                    title = "Use phone lock",
                    subtitle = if (method == UnlockMethod.DEVICE) "Selected" else "Fingerprint or the phone PIN",
                    onClick = {
                        if (!biometric.isAvailable()) {
                            message = "Set a screen lock or fingerprint on this phone first."
                        } else if (repository.hasBiometricWrap()) {
                            repository.setUnlockMethod(UnlockMethod.DEVICE)
                            method = UnlockMethod.DEVICE
                        } else {
                            enrolling = true
                        }
                    },
                )
                HorizontalDivider(color = Color(0xFFEEF1F6))
                SettingsRow(
                    icon = Icons.Default.Pin,
                    title = if (hasPin) "Update PIN" else "Use a vault PIN",
                    subtitle = if (hasPin) pinUpdatedLabel(repository.pinChangedAt()) else "6-digit PIN and a recovery passphrase",
                    onClick = {
                        if (hasPin) {
                            repository.setUnlockMethod(UnlockMethod.PIN)
                            method = UnlockMethod.PIN
                            backStack.add(Destinations.ChangePin)
                        } else {
                            creatingPin = true
                        }
                    },
                )
                HorizontalDivider(color = Color(0xFFEEF1F6))
                SettingsRow(
                    icon = Icons.Default.NoEncryption,
                    title = "No lock",
                    subtitle = if (method == UnlockMethod.NONE) "Selected" else "Open the vault with no prompt",
                    onClick = { confirmNone = true },
                )
            }
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = VaultRed, fontSize = 13.sp)
        }
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
