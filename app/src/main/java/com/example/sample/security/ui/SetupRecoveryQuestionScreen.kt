package com.example.sample.security.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sample.security.RecoveryQuestion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupRecoveryQuestionScreen(
    error: String?,
    onBack: () -> Unit,
    onComplete: (recoveryQuestionId: Int, recoveryAnswer: String) -> Unit,
) {
    var question by remember { mutableStateOf(RecoveryQuestion.FIRST_PET) }
    var answer by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var showAnswer by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    val matches = answer.length >= 3 && answer.trim().equals(confirm.trim(), ignoreCase = true)

    Column(Modifier.fillMaxSize().background(VaultBackground)) {
        VaultHeader("Setup Recovery Question", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            SecurityHero(
                title = "Security Recovery Question",
                description = "If you ever forget your vault PIN, this question will be required alongside your master key to restore access.",
                icon = Icons.Default.Security,
            )
            VaultSurface {
                Text("Recovery Question *", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = question.text,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        shape = RoundedCornerShape(12.dp),
                    )
                    ExposedDropdownMenu(expanded, onDismissRequest = { expanded = false }) {
                        RecoveryQuestion.entries.forEach { candidate ->
                            DropdownMenuItem(
                                text = { Text(candidate.text) },
                                onClick = {
                                    question = candidate
                                    expanded = false
                                    answer = ""
                                    confirm = ""
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text("Your Secret Answer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("Min 3 characters", color = VaultSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = answer,
                    onValueChange = { answer = it; localError = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Oakridge Elementary") },
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
                Text(
                    "Answers are case-insensitive and hashed with Argon2id before physical storage.",
                    color = VaultSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text("Confirm Secret Answer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; localError = null },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Oakridge Elementary") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(12.dp),
                )
                if (matches) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF067647), modifier = Modifier.height(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Answers match perfectly", color = Color(0xFF067647), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = VaultRedBackground) {
                Column(Modifier.padding(16.dp)) {
                    Text("Irreversible Security", color = VaultRed, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Fortress Vault administrators cannot retrieve this answer for you. Keep a secure physical backup in an offline safe or emergency document kit.",
                        color = VaultRed,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    )
                }
            }
            (localError ?: error)?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = VaultRed, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(14.dp))
            VaultPrimaryButton("Complete Setup & Lock Vault", Icons.Default.Lock) {
                localError = when {
                    answer.trim().length < 3 -> "Your recovery answer must have at least 3 characters."
                    !matches -> "Recovery answers do not match."
                    else -> null
                }
                if (localError == null) onComplete(question.id, answer.trim())
            }
            TextButton(onClick = { expanded = true }) {
                Text("Choose a custom question instead", color = VaultSecondary)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}
