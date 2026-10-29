package com.example.sample.security.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RecoverPinScreen(
    questionText: String,
    error: String?,
    onSubmit: (String) -> Unit,
    onBack: () -> Unit,
) {
    var answer by remember { mutableStateOf("") }
    var showAnswer by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(VaultBackground)) {
        VaultHeader("Recover PIN with Question", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SecurityHero(
                title = "Answer Security Question",
                description = "Verify your identity using the recovery question configured during PIN setup to reset your master PIN.",
                icon = Icons.Default.Security,
            )
            VaultSurface {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, null, tint = VaultNavy, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("SECURITY QUESTION", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = VaultSecondary, modifier = Modifier.weight(1f))
                    Text("1 of 2 Configured", color = VaultNavy, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF0F3F7)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, null, tint = VaultNavy)
                        Spacer(Modifier.width(10.dp))
                        Text(questionText, fontSize = 14.sp, color = VaultText)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Your Recovery Answer *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your exact answer") },
                    singleLine = true,
                    visualTransformation = if (showAnswer) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showAnswer = !showAnswer }) {
                            Icon(if (showAnswer) Icons.Default.VisibilityOff else Icons.Default.Visibility, "Toggle answer")
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(8.dp))
                Text("Hashed locally. Never transmitted unencrypted.", color = VaultSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (error == null) "Attempts remaining" else error,
                        color = if (error == null) Color(0xFF067647) else VaultRed,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f),
                    )
                    Text("3 of 3 permitted", color = VaultSecondary, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    repeat(3) {
                        Spacer(Modifier.width(4.dp))
                        androidx.compose.foundation.layout.Box(
                            Modifier.size(8.dp).clip(CircleShape).background(if (error == null) Color(0xFF12B76A) else VaultRed),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            VaultPrimaryButton("Verify Answer & Reset PIN") { onSubmit(answer.trim()) }
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = Color(0xFFEBEEF2)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Don't remember your recovery answer?", color = VaultSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, null, tint = VaultNavy, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("Emergency Master Recovery Kit", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Use your offline 24-word paper key.", color = VaultSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
