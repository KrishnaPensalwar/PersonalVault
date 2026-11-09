package com.example.sample.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sample.navigation.LocalBackStack
import com.example.sample.security.ChangePinViewModel
import com.example.sample.security.ui.VaultPrimaryButton
import com.example.sample.security.ui.VaultRed
import com.example.sample.security.ui.VaultSecondary
import org.koin.androidx.compose.koinViewModel

@Composable
fun ChangePinScreen(viewModel: ChangePinViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsState()
    val backStack = LocalBackStack.current
    var current by rememberSaveable { mutableStateOf("") }
    var next by rememberSaveable { mutableStateOf("") }
    var confirm by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(state.saved) {
        if (state.saved) backStack.removeLastOrNull()
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            "Enter the current PIN, then a new 6-digit PIN. The vault key stays the same. Only its PIN wrap changes.",
            color = VaultSecondary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(16.dp))
        PinField("Current PIN", current) { current = it.filter(Char::isDigit).take(6) }
        Spacer(Modifier.height(12.dp))
        PinField("New PIN", next) { next = it.filter(Char::isDigit).take(6) }
        Spacer(Modifier.height(12.dp))
        PinField("Confirm new PIN", confirm) { confirm = it.filter(Char::isDigit).take(6) }
        state.error?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = VaultRed, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        VaultPrimaryButton("Save new PIN") { viewModel.onSubmit(current, next, confirm) }
    }
}

@Composable
private fun PinField(label: String, value: String, onValue: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
    )
}
