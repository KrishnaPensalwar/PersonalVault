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
import androidx.compose.material3.lightColorScheme
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

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = VaultNavy,
            background = VaultBackground,
            surface = VaultCard,
            onSurface = VaultText,
            onBackground = VaultText,
        ),
    ) {
        Column(
            modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues())
        ) {
            when (val current = state) {
                LockScreenState.Checking -> Box(
                    Modifier
                        .fillMaxSize()
                        .background(VaultBackground),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = VaultNavy) }

                LockScreenState.Biometric -> BiometricUnlockScreen(
                    onSuccess = viewModel::onBiometricSuccess,
                    onError = viewModel::onBiometricFailed,
                )

                is LockScreenState.PinEntry -> EnterVaultPinScreen(
                    error = current.error,
                    onSubmit = viewModel::onPinEntrySubmit,
                    onForgotPin = viewModel::onForgotPinClicked,
                )

                is LockScreenState.PinSetup -> PinSetupFlow(
                    error = current.error,
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

                LockScreenState.Unlocked -> Unit
            }
        }

    }
}
