package com.example.sample.security.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Reusable 0-9 + backspace numeric keypad used by every PIN screen. */
@Composable
fun PinKeypad(
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
    )
    Column(modifier = modifier, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                row.forEach { digit ->
                    OutlinedButton(onClick = { onDigit(digit) }, modifier = Modifier.padding(4.dp).size(64.dp)) {
                        Text(digit.toString())
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.size(64.dp).padding(4.dp))
            OutlinedButton(onClick = { onDigit('0') }, modifier = Modifier.padding(4.dp).size(64.dp)) {
                Text("0")
            }
            OutlinedButton(onClick = onBackspace, modifier = Modifier.padding(4.dp).size(64.dp)) {
                Text("⌫")
            }
        }
    }
}
