package com.example.sample.storage.documentDetailScreen

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import com.example.sample.common.ErrorState
import com.example.sample.common.LoadingState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.sample.navigation.SetTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.loadDocumentPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel

@Composable
fun DocumentDetailScreen(
    id: String,
    viewModel: DocumentDetailViewModel = koinViewModel()
) {
    val backStack = LocalBackStack.current
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(id) {
        viewModel.loadDocument(id)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
            when (val state = uiState) {
                is DocumentDetailUiState.Loading -> {
                    LoadingState(title = "Loading Document Details...")
                }

                is DocumentDetailUiState.Error -> {
                    ErrorState(
                        title = "Error Loading Document",
                        description = state.message,
                        image = Icons.AutoMirrored.Filled.InsertDriveFile,
                        onRetry = { viewModel.loadDocument(id) },
                        onBack = { backStack.removeLastOrNull() }
                    )
                }

                is DocumentDetailUiState.Success -> {
                    DocumentDetailContent(
                        doc = state.document,
                        onUpdateName = { newName ->
                            viewModel.updateDocumentName(id, newName, onSuccess = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Document name updated successfully")
                                }
                            })
                        },
                        onDelete = {
                            viewModel.deleteDocument(id, onSuccess = {
                                backStack.removeLastOrNull()
                            })
                        },
                        onBack = {
                            backStack.removeLastOrNull()
                        },
                        onViewFullDocument = {
                            backStack.add(Destinations.DocumentViewer(id = id))
                        }
                    )
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun DocumentDetailContent(
    doc: VaultDocumentEntity,
    onUpdateName: (String) -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    onViewFullDocument: () -> Unit
) {
    var editableName by remember(doc.name) { mutableStateOf(doc.name) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val nameChanged = editableName.isNotBlank() && editableName != doc.name

    BackHandler(enabled = nameChanged) {
        showDiscardDialog = true
    }

    val context = LocalContext.current
    val preview by produceState<Bitmap?>(
        initialValue = null,
        key1 = doc.relativePath,
        key2 = doc.mimeType
    ) {
        value = withContext(Dispatchers.IO) {
            loadDocumentPreview(context, doc)
        }
    }

    SetTopBar(
        title = doc.name,
        onBackClick = {
            if (nameChanged) {
                showDiscardDialog = true
            } else {
                onBack()
            }
        },
        actions = {
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete Document",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .fillMaxWidth()
                    .height(280.dp)
                    .clickable { onViewFullDocument() },
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview!!.asImageBitmap(),
                        contentDescription = "Document Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = "Document Preview",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            DocumentDetailRow("Name", editableName, enabled = true, onValueChange = {
                editableName = it
            })

            Spacer(Modifier.height(16.dp))

            DocumentDetailRow(
                "Type",
                doc.mimeType?.substringAfter("/")?.uppercase() ?: "UNKNOWN"
            )

            Spacer(Modifier.height(16.dp))

            DocumentDetailRow("Size", "%.2f KB".format((doc.sizeBytes) / 1024.0))

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onUpdateName(editableName)
                },
                enabled = nameChanged,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Name")
            }
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("You have unsaved changes. Are you sure you want to go back?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        onBack()
                    }
                ) {
                    Text("Discard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onConfirm = {
                showDeleteDialog = false
                onDelete()
            },
            onDismiss = {
                showDeleteDialog = false
            }
        )
    }
}

@Composable
fun DocumentDetailRow(
    title: String,
    value: String,
    onValueChange: (String) -> Unit = {},
    enabled: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(title) },
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true
    )
}

@Composable
fun DeleteConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Document") },
        text = { Text("Are you sure you want to delete this document? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
