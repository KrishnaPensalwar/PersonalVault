package com.example.sample.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.security.AppLockViewModel
import com.example.sample.security.LockScreenState
import com.example.sample.security.RecoveryQuestion

@Composable
fun LockScreen(viewModel: AppLockViewModel) {
    val state by viewModel.lockState.collectAsStateWithLifecycle()

    when (val current = state) {
        is LockScreenState.Checking -> Unit

        is LockScreenState.Biometric -> BiometricUnlockScreen(
            onSuccess = viewModel::onBiometricSuccess,
            onError = viewModel::onBiometricFailed,
        )

        is LockScreenState.PinEntry -> PinEntryContent(
            error = current.error,
            onSubmit = viewModel::onPinEntrySubmit,
            onForgotPin = viewModel::onForgotPinClicked,
        )

        is LockScreenState.PinSetup -> PinSetupContent(
            error = current.error,
            onSubmit = viewModel::onPinSetupSubmit,
        )

        is LockScreenState.RecoveryPrompt -> RecoveryQuestionContent(
            questionText = current.question.text,
            error = current.error,
            onSubmit = viewModel::onRecoveryAnswerSubmit,
            onBack = viewModel::onBackToPinEntry,
        )

        is LockScreenState.SetNewPin -> SetNewPinContent(
            error = current.error,
            onSubmit = viewModel::onSetNewPinSubmit,
        )

        is LockScreenState.Unlocked -> Unit
    }
}

@Composable
private fun PinEntryContent(error: String?, onSubmit: (String) -> Unit, onForgotPin: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Enter your PIN")
        Text("● ".repeat(pin.length).trim())
        error?.let { Text(it) }
        PinKeypad(
            onDigit = { digit ->
                if (pin.length < 4) {
                    pin += digit
                    if (pin.length == 4) {
                        onSubmit(pin)
                        pin = ""
                    }
                }
            },
            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) },
            modifier = Modifier.padding(top = 16.dp),
        )
        TextButton(onClick = onForgotPin, modifier = Modifier.padding(top = 8.dp)) {
            Text("Forgot PIN?")
        }
    }
}

@Composable
private fun PinSetupContent(
    error: String?,
    onSubmit: (pin: String, confirmPin: String, recoveryQuestionId: Int, recoveryAnswer: String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var question by remember { mutableStateOf(RecoveryQuestion.FIRST_PET) }
    var answer by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Set a 4-digit PIN")
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4) pin = it.filter(Char::isDigit) }, label = { Text("PIN") })
        OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 4) confirmPin = it.filter(Char::isDigit) }, label = { Text("Confirm PIN") })
        Text("Recovery question: ${question.text}")
        RecoveryQuestion.entries.forEach { candidate ->
            TextButton(onClick = { question = candidate }) { Text(candidate.text) }
        }
        OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Your answer") })
        error?.let { Text(it) }
        TextButton(onClick = { onSubmit(pin, confirmPin, question.id, answer) }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Save")
        }
    }
}

@Composable
private fun RecoveryQuestionContent(
    questionText: String,
    error: String?,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(questionText)
        OutlinedTextField(value = answer, onValueChange = { answer = it }, label = { Text("Your answer") })
        error?.let { Text(it) }
        TextButton(onClick = { onSubmit(answer) }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Submit")
        }
        TextButton(onClick = onBack) { Text("Back") }
    }
}

@Composable
private fun SetNewPinContent(
    error: String?,
    onSubmit: (pin: String, confirmPin: String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Set a new 4-digit PIN")
        OutlinedTextField(value = pin, onValueChange = { if (it.length <= 4) pin = it.filter(Char::isDigit) }, label = { Text("New PIN") })
        OutlinedTextField(value = confirmPin, onValueChange = { if (it.length <= 4) confirmPin = it.filter(Char::isDigit) }, label = { Text("Confirm PIN") })
        error?.let { Text(it) }
        TextButton(onClick = { onSubmit(pin, confirmPin) }, modifier = Modifier.padding(top = 16.dp)) {
            Text("Save")
        }
    }
}
