package com.example.sample.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.storage.data.VaultDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(
    viewModel: StorageViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedDocument by remember { mutableStateOf<SelectedDocument?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var editableName by remember { mutableStateOf("") }
    
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isConfirmedToDismiss by remember { mutableStateOf(false) }

    val documents by viewModel.documents.collectAsState()

    val backStack = LocalBackStack.current

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden && !isConfirmedToDismiss) {
                showDiscardDialog = true
                false
            } else {
                true
            }
        }
    )

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val docInfo = getDocumentInfo(context, it)
            selectedDocument = docInfo
            selectedUri = it
            editableName = docInfo.fileName
            isConfirmedToDismiss = false
            showBottomSheet = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Storage",
                fontSize = 24.sp
            )

            Button(
                onClick = {
                    documentPicker.launch(
                        arrayOf("*/*")
                    )
                }
            ) {
                Text("Add")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            items(documents) { doc ->
                StorageDocumentCard(
                    document = doc,
                    onClick = {
                        backStack.add(Destinations.DocumentDetailScreen(id = doc.id))
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showBottomSheet) {
        DocumentSaveBottomSheet(
            sheetState = sheetState,
            selectedDocument = selectedDocument,
            selectedUri = selectedUri,
            editableName = editableName,
            onNameChange = { editableName = it },
            onDismissRequest = {
                if (!isConfirmedToDismiss) {
                    showDiscardDialog = true
                }
            },
            onSave = {
                val uri = selectedUri
                val doc = selectedDocument
                if (uri != null && doc != null) {
                    val path = copyDocumentToInternalStorage(context, uri)
                    viewModel.saveDocument(
                        VaultDocumentEntity(
                            id = UUID.randomUUID().toString(),
                            name = editableName.ifBlank { doc.fileName },
                            relativePath = path,
                            mimeType = doc.mimeType,
                            sizeBytes = doc.size ?: 0L
                        )
                    )
                }

                isConfirmedToDismiss = true
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    showBottomSheet = false
                    selectedUri = null
                    selectedDocument = null
                }
            }
        )
    }

    if (showDiscardDialog) {
        DiscardDocumentDialog(
            onDismiss = { showDiscardDialog = false },
            onConfirm = {
                showDiscardDialog = false
                isConfirmedToDismiss = true
                scope.launch {
                    sheetState.hide()
                }.invokeOnCompletion {
                    showBottomSheet = false
                    selectedUri = null
                    selectedDocument = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentSaveBottomSheet(
    sheetState: SheetState,
    selectedDocument: SelectedDocument?,
    selectedUri: Uri?,
    editableName: String,
    onNameChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    
    val preview by produceState<Bitmap?>(
        initialValue = null,
        key1 = selectedUri,
        key2 = selectedDocument?.mimeType
    ) {
        value = withContext(Dispatchers.IO) {
            if (selectedUri != null && selectedDocument != null) {
                loadDocumentPreviewFromUri(context, selectedUri.toString(), selectedDocument.mimeType)
            } else null
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Save Document",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview!!.asImageBitmap(),
                        contentDescription = "Document Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
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

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = editableName,
                onValueChange = onNameChange,
                label = { Text("Document Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(16.dp))

            if (selectedDocument != null) {
                val sizeKb = (selectedDocument.size ?: 0) / 1024.0
                Text(
                    text = "Type: ${selectedDocument.mimeType?.substringAfter("/")?.uppercase() ?: "UNKNOWN"} • Size: %.2f KB".format(sizeKb),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Done")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun DiscardDocumentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard Document?") },
        text = { Text("Are you sure you want to discard this document? It won't be saved.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Discard")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun loadDocumentPreviewFromUri(
    context: Context,
    uri: String,
    mimeType: String?
): Bitmap? {
    return try {
        when {
            mimeType?.startsWith("image/") == true -> {
                context.contentResolver.openInputStream(uri.toUri())?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
            mimeType == "application/pdf" -> {
                val fd = context.contentResolver.openFileDescriptor(uri.toUri(), "r")
                fd?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (renderer.pageCount == 0) return null
                        renderer.openPage(0).use { page ->
                            val targetWidth = 400
                            val scale = targetWidth.toFloat() / page.width
                            val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = createBitmap(targetWidth, targetHeight)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            return bitmap
                        }
                    }
                }
            }
            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
