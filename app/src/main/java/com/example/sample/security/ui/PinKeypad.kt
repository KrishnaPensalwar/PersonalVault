package com.example.sample.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val KeyLetters = mapOf(
    '2' to "ABC", '3' to "DEF", '4' to "GHI", '5' to "JKL",
    '6' to "MNO", '7' to "PQRS", '8' to "TUV", '9' to "WXYZ", '0' to "+",
)

@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
    onBiometric: (() -> Unit)? = null,
) {
    val rows = listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9'))
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { digit ->
                    KeypadNumber(digit, Modifier.weight(1f)) { onDigit(digit) }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KeypadAction(Modifier.weight(1f), enabled = onBiometric != null, onClick = { onBiometric?.invoke() }) {
                Icon(Icons.Default.Fingerprint, "Use fingerprint", tint = VaultNavy, modifier = Modifier.size(28.dp))
            }
            KeypadNumber('0', Modifier.weight(1f)) { onDigit('0') }
            KeypadAction(Modifier.weight(1f), enabled = true, onClick = onBackspace) {
                Icon(Icons.AutoMirrored.Filled.Backspace, "Delete digit", tint = VaultText, modifier = Modifier.size(25.dp))
            }
        }
    }
}

@Composable
private fun KeypadNumber(digit: Char, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(22.dp),
        color = VaultCard,
        shadowElevation = 1.dp,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(digit.toString(), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = VaultText)
//            Text(KeyLetters[digit].orEmpty(), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color(0xFF777C87))
        }
    }
}

@Composable
private fun KeypadAction(
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(72.dp),
        shape = RoundedCornerShape(22.dp),
        color = if (enabled) Color(0xFFF0F2F5) else VaultCard,
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}
