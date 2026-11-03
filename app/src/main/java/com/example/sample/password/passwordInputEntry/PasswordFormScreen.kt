package com.example.sample.password.passwordInputEntry

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import com.example.sample.common.AppMessages
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.navigation.LocalBackStack
import com.example.sample.navigation.SetTopBar
import com.example.sample.password.domain.PasswordFormCategories
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel
import kotlin.random.Random

private val Navy = Color(0xFF1B2559)
private val Page = Color(0xFFF7F8FC)
private val Field = Color.White
private val Muted = Color(0xFF8B93A7)
private val Green = Color(0xFF1F8A4C)

@Composable
fun PasswordFormScreen(
    passwordId: String? = null,
    viewModel: PasswordFormViewModel = koinViewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val backStack = LocalBackStack.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var passwordVisible by remember { mutableStateOf(false) }
    var baseline by remember(passwordId) { mutableStateOf<FormDraft?>(null) }
    var showLeaveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(passwordId) {
        if (passwordId != null) viewModel.initEditForm(passwordId) else viewModel.initAddForm()
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PasswordFormEvent.SavedSuccessfully -> {
                    AppMessages.show(if (formState.isEditMode) "Password updated" else "Password saved")
                    backStack.removeLastOrNull()
                }
                is PasswordFormEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    LaunchedEffect(passwordId, formState.id, formState.isEditMode) {
        if (baseline != null) return@LaunchedEffect
        val ready = passwordId == null || (formState.isEditMode && formState.id == passwordId)
        if (ready) baseline = formState.toDraft()
    }

    val dirty = baseline != null && formState.toDraft() != baseline
    val leave: () -> Unit = { backStack.removeLastOrNull() }

    BackHandler(enabled = dirty) { showLeaveDialog = true }

    SetTopBar(
        title = if (formState.isEditMode) "Edit Password" else "Add Password",
        onBackClick = { if (dirty) showLeaveDialog = true else leave() },
    )

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Field,
        unfocusedContainerColor = Field,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        cursorColor = Navy,
    )
    val notesFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Page,
        unfocusedContainerColor = Page,
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        cursorColor = Navy,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Page),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            FieldLabel("Title / Service", required = true)
            OutlinedTextField(
                value = formState.title,
                onValueChange = viewModel::onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("e.g. Work, Cloud, Bank", color = Muted) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = null,
                        tint = Muted
                    )
                },
                isError = formState.titleError != null,
                supportingText = formState.titleError?.let { { Text(it) } },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
            )

            FieldLabel("Category")
            CategoryChips(selected = formState.category, onSelected = viewModel::onCategoryChanged)

            FieldLabel("Username or Email", required = true)
            OutlinedTextField(
                value = formState.username,
                onValueChange = viewModel::onUsernameChanged,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Muted
                    )
                },
                isError = formState.usernameError != null,
                supportingText = formState.usernameError?.let { { Text(it) } },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                placeholder = { Text("Enter Username", color = Muted) })

            Row(verticalAlignment = Alignment.CenterVertically) {
                FieldLabel("Password", required = true, modifier = Modifier.weight(1f))
                TextButton(onClick = { viewModel.onPasswordChanged(generateStrongPassword()) }) {
                    Text(
                        "Generate Strong",
                        color = Color(0xFF3D5AFE),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            OutlinedTextField(
                value = formState.password,
                onValueChange = viewModel::onPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Muted) },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                tint = Muted,
                            )
                        }
                        IconButton(onClick = {
                            copy(context, formState.password)
                        }) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Copy password",
                                tint = Muted
                            )
                        }
                    }
                },
                isError = formState.passwordError != null,
                supportingText = formState.passwordError?.let { { Text(it) } },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                placeholder = { Text("Enter Password", color = Muted) }

            )
//            if (false) StrengthMeter(formState.password)

            FieldLabel("Website URL")
            OutlinedTextField(
                value = formState.websiteUrl,
                onValueChange = viewModel::onWebsiteUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null, tint = Muted) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = fieldColors,
                placeholder = { Text("Enter Url", color = Muted) }

            )

            if (false) InfoCard(
                icon = Icons.Default.Lock,
                title = "Save To Vault",
                body = "Personal Vault (Only you)",
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Secure Notes & Recovery",
                            fontWeight = FontWeight.SemiBold,
                            color = Navy
                        )
                        Spacer(Modifier.weight(1f))
                        if (false) Text(
                            "Encrypted client-side",
                            color = Green,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color(0xFFDDF6EA))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = formState.notes,
                        onValueChange = viewModel::onNotesChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(96.dp),
                        placeholder = { Text("Optional notes", color = Muted) },
                        shape = RoundedCornerShape(12.dp),
                        colors = notesFieldColors,
                    )
                }
            }

            if (dirty) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF4D6),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (formState.isEditMode) {
                            "You have unsaved changes. Save before leaving."
                        } else {
                            "You have unsaved entries. Save before leaving."
                        },
                        color = Color(0xFF8A5A00),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(14.dp),
                    )
                }
            }

            ToggleRow(
                icon = Icons.Default.Star,
                title = "Mark as Favorite",
                subtitle = "Pin to quick access in Family Vault",
                checked = formState.isFavorite,
                onChecked = { viewModel.onFavoriteToggled() },
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = viewModel::onSavePassword,
                enabled = !formState.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Navy,
                    contentColor = Color.White
                ),
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (formState.isEditMode) "Update Password" else "Save Password to Vault",
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            TextButton(
                onClick = { if (dirty) showLeaveDialog = true else leave() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text("Discard & Return", color = Muted)
            }
            Spacer(Modifier.height(12.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { showLeaveDialog = false },
            title = { Text(if (formState.isEditMode) "Discard changes?" else "Discard entry?") },
            text = {
                Text(
                    if (formState.isEditMode) {
                        "Updates are not saved yet."
                    } else {
                        "This password is not saved yet."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLeaveDialog = false
                    leave()
                }) { Text("Leave", color = Color(0xFFE11D48)) }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveDialog = false }) { Text("Stay") }
            },
        )
    }
}

private data class FormDraft(
    val title: String,
    val username: String,
    val password: String,
    val websiteUrl: String,
    val notes: String,
    val category: String,
    val isFavorite: Boolean,
)

private fun PasswordFormState.toDraft() = FormDraft(
    title = title,
    username = username,
    password = password,
    websiteUrl = websiteUrl,
    notes = notes,
    category = category,
    isFavorite = isFavorite,
)

@Composable
private fun FieldLabel(text: String, required: Boolean = false, modifier: Modifier = Modifier) {
    Row(modifier) {
        Text(text, fontWeight = FontWeight.SemiBold, color = Navy, fontSize = 14.sp)
        if (required) Text(" *", color = Color(0xFFE11D48), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CategoryChips(selected: String, onSelected: (String) -> Unit) {
    val extras = if (selected in PasswordFormCategories) emptyList() else listOf(selected)
    val options = PasswordFormCategories + extras
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { category ->
                    val on = category == selected
                    Surface(
                        onClick = { onSelected(category) },
                        shape = RoundedCornerShape(50),
                        color = if (on) Navy else Color.White,
                    ) {
                        Text(
                            category,
                            color = if (on) Color.White else Navy,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StrengthMeter(password: String) {
    val classes = listOf(
        password.any { it.isLowerCase() },
        password.any { it.isUpperCase() },
        password.any { it.isDigit() },
        password.any { !it.isLetterOrDigit() },
    ).count { it }
    val (label, color, filled) = when {
        password.isEmpty() -> Triple("Empty", Muted, 0)
        password.length >= 16 && classes >= 3 -> Triple("Very Strong", Green, 4)
        password.length >= 12 && classes >= 3 -> Triple("Strong", Green, 3)
        password.length >= 8 && classes >= 2 -> Triple("Fair", Color(0xFFE6A700), 2)
        else -> Triple("Weak", Color(0xFFE11D48), 1)
    }
    val parts = buildList {
        add("${password.length} characters")
        if (password.any { it.isLetter() }) add("Letters")
        if (password.any { it.isDigit() }) add("Numbers")
        if (password.any { !it.isLetterOrDigit() }) add("Symbols")
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = color, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(parts.joinToString(" • "), color = Muted, fontSize = 12.sp)
    }
    Spacer(Modifier.height(6.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
        repeat(4) { index ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (index < filled) color else Color(0xFFE4E7EE)),
            )
        }
    }
}

@Composable
private fun InfoCard(icon: ImageVector, title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF3D5AFE))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, color = Muted, fontSize = 12.sp)
                Text(body, color = Navy, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onChecked: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF3D5AFE))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Navy, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = { onChecked() },
                colors = SwitchDefaults.colors(checkedTrackColor = Navy),
            )
        }
    }
}

private fun copy(context: Context, secret: String) {
    if (secret.isEmpty()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("password", secret))
}

private fun generateStrongPassword(): String {
    val chars = "abcdefghijkmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789!@#\$%&*"
    return buildString(19) { repeat(19) { append(chars[Random.nextInt(chars.length)]) } }
}
