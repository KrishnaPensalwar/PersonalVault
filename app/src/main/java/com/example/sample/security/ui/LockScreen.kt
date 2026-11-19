package com.example.sample.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.security.AppLockViewModel
import com.example.sample.security.LockScreenState

@Composable
fun LockScreen(viewModel: AppLockViewModel) {
    val state by viewModel.lockState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
            when (val current = state) {
                LockScreenState.Checking -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(VaultBackground),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = VaultNavy) }

                LockScreenState.Biometric -> BiometricUnlockScreen(
                    cipher = viewModel.biometricUnlockCipher(),
                    enroll = false,
                    allowDeviceCredential = viewModel.allowsDeviceCredential(),
                    onAuthorized = viewModel::onBiometricUnlock,
                    onError = viewModel::onBiometricFailed,
                )

                LockScreenState.BiometricEnroll -> BiometricUnlockScreen(
                    cipher = viewModel.biometricEnrollCipher(),
                    enroll = true,
                    allowDeviceCredential = viewModel.allowsDeviceCredential(),
                    dismissOnCancel = true,
                    onAuthorized = viewModel::onBiometricEnroll,
                    onCancel = { viewModel.onBiometricFailed("Device lock was cancelled.") },
                    onError = viewModel::onBiometricFailed,
                )

                is LockScreenState.PinEntry -> EnterVaultPinScreen(
                    error = current.error,
                    failedAttempts = current.failedAttempts,
                    checking = current.checking,
                    onSubmit = viewModel::onPinEntrySubmit,
                    onForgotPin = viewModel::onForgotPinClicked,
                )

                is LockScreenState.PinSetup -> PinSetupFlow(
                    error = current.error,
                    saving = current.saving,
                    onSubmit = viewModel::onPinSetupSubmit,
                )

                is LockScreenState.RecoveryPrompt -> RecoverPinScreen(
                    questionText = current.question.text,
                    error = current.error,
                    onSubmit = viewModel::onRecoveryAnswerSubmit,
                    onBack = viewModel::onBackToPinEntry,
                )

                is LockScreenState.SetNewPin -> SetNewPinScreen(
                    error = current.error,
                    onSubmit = viewModel::onSetNewPinSubmit,
                )

                is LockScreenState.ChooseUnlock -> ChooseUnlockScreen(
                    error = current.error,
                    onDevice = viewModel::onChooseDevice,
                    onPin = viewModel::onChoosePin,
                    onNone = viewModel::onChooseNone,
                )

                LockScreenState.Unlocked -> Unit
            }
    }
}
