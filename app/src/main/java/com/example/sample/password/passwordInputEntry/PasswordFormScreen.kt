package com.example.sample.password.passwordInputEntry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.navigation.LocalBackStack
import com.example.sample.password.domain.PasswordCategories
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordFormScreen(
    passwordId: String? = null,
    viewModel: PasswordFormViewModel = koinViewModel(),
) {
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val backStack = LocalBackStack.current
    val snackbarHostState = remember { SnackbarHostState() }
    var passwordVisible by remember { mutableStateOf(false) }

    // Init form mode
    LaunchedEffect(passwordId) {
        if (passwordId != null) {
            viewModel.initEditForm(passwordId)
        } else {
            viewModel.initAddForm()
        }
    }

    // Collect events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PasswordFormEvent.SavedSuccessfully -> backStack.removeLastOrNull()
                is PasswordFormEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (formState.isEditMode) "Edit Password" else "Add Password",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { backStack.removeLastOrNull() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets(0,0,0,0)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            // ── Title ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = formState.title,
                onValueChange = viewModel::onTitleChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title *") },
                placeholder = { Text("e.g. GitHub") },
                isError = formState.titleError != null,
                supportingText = {
                    formState.titleError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
            )

            // ── Username ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = formState.username,
                onValueChange = viewModel::onUsernameChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Username / Email *") },
                placeholder = { Text("e.g. user@example.com") },
                isError = formState.usernameError != null,
                supportingText = {
                    formState.usernameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
            )

            // ── Password ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = formState.password,
                onValueChange = viewModel::onPasswordChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Password *") },
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        )
                    }
                },
                isError = formState.passwordError != null,
                supportingText = {
                    formState.passwordError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                singleLine = true,
            )

            // ── Website URL ──────────────────────────────────────────────────
            OutlinedTextField(
                value = formState.websiteUrl,
                onValueChange = viewModel::onWebsiteUrlChanged,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Website URL") },
                placeholder = { Text("https://example.com") },
                singleLine = true,
            )

            // ── Category dropdown ────────────────────────────────────────────
            CategoryDropdown(
                selected = formState.category,
                onSelected = viewModel::onCategoryChanged,
            )

            // ── Notes ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = formState.notes,
                onValueChange = viewModel::onNotesChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                label = { Text("Notes") },
                placeholder = { Text("Optional notes…") },
                maxLines = 5,
            )

            // ── Favourite toggle ─────────────────────────────────────────────
            FilterChip(
                selected = formState.isFavorite,
                onClick = viewModel::onFavoriteToggled,
                label = { Text("Mark as Favourite") },
                leadingIcon = {
                    Icon(
                        if (formState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (formState.isFavorite) Color(0xFFE91E63)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )

            Spacer(Modifier.height(4.dp))

            // ── Save button ──────────────────────────────────────────────────
            Button(
                onClick = viewModel::onSavePassword,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = !formState.isSaving,
                shape = MaterialTheme.shapes.medium,
            ) {
                if (formState.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(
                        text = if (formState.isEditMode) "Update Password" else "Save Password",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Category dropdown ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    selected: String,
    onSelected: (String) -> Unit,
) {
    // Exclude "All" from the dropdown (it's a filter-only category)
    val options = PasswordCategories.drop(1)
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category) },
                    onClick = {
                        onSelected(category)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
