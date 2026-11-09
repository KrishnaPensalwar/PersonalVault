package com.example.sample.storage.documentsListingScreen

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.common.EmptyState
import com.example.sample.common.ErrorState
import com.example.sample.common.LoadingState
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.storage.SelectedDocument
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.getDocumentInfo
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageScreen(viewModel: StorageViewModel = koinViewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backStack = LocalBackStack.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedDocument by remember { mutableStateOf<SelectedDocument?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var editableName by remember { mutableStateOf("") }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isConfirmedToDismiss by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf("All Files") }
    var sortAz by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }
    var source by remember { mutableStateOf(VaultSource.Upload) }
    var noteBody by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<VaultDocumentEntity?>(null) }
    var pendingScanUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(searchQuery) { viewModel.getDocuments(searchQuery) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { newValue ->
            if (newValue == SheetValue.Hidden && !isConfirmedToDismiss) {
                showDiscardDialog = true
                false
            } else {
                true
            }
        },
    )

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
            val uri = pendingScanUri ?: return@rememberLauncherForActivityResult
            if (!saved) return@rememberLauncherForActivityResult
            val name = "Scan_${System.currentTimeMillis()}.jpg"
            val length = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            }.getOrNull() ?: 0L
            selectedUri = uri
            selectedDocument = SelectedDocument(uri.toString(), length, name, "image/jpeg")
            editableName = name
            source = VaultSource.Scan
        }

    fun launchScan() {
        val scans = File(context.cacheDir, "scans").apply { mkdirs() }
        val file = File(scans, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingScanUri = uri
        takePicture.launch(uri)
    }

    val cameraPermission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchScan()
            else scope.launch { snackbarHostState.showSnackbar("Camera permission needed to scan") }
        }

    val documentPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri ?: return@rememberLauncherForActivityResult
            val docInfo = getDocumentInfo(context, uri)
            selectedDocument = docInfo
            selectedUri = uri
            editableName = docInfo.fileName
            source = VaultSource.Upload
            isConfirmedToDismiss = false
            showBottomSheet = true
        }

    fun resolvedCategory() =
        if (selectedCategory == OtherCategory) customCategory.trim() else selectedCategory.trim()

    fun hideSheet() {
        isConfirmedToDismiss = true
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            showBottomSheet = false
            selectedUri = null
            selectedDocument = null
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val state = uiState) {
            is StorageUiState.Loading -> LoadingState(title = "Loading Documents...")
            is StorageUiState.Error -> ErrorState(
                title = "Error Loading Documents",
                description = state.message,
                image = Icons.Default.Storage,
                onRetry = { viewModel.getDocuments(searchQuery) },
            )
            is StorageUiState.Success -> if (
                state.documents.isEmpty()
            ) EmptyState(
                title = if (searchQuery.isBlank() && categoryFilter == "All Files") {
                    "No documents saved yet"
                } else {
                    "No matching documents"
                },
                description = "Tap + to add a document",
                image = Icons.Default.Storage,
            ) else StorageDocumentList(
                documents = state.documents,
                searchQuery = searchQuery,
                categoryFilter = categoryFilter,
                sortAz = sortAz,
                onCategoryFilter = { categoryFilter = it },
                onSearchChange = { searchQuery = it; viewModel.getDocuments(it) },
                onClearSearch = { searchQuery = ""; viewModel.getDocuments() },
                onToggleSort = { sortAz = !sortAz },
                onOpen = { backStack.add(Destinations.DocumentViewer(id = it)) },
                onEdit = { backStack.add(Destinations.DocumentDetailScreen(id = it)) },
                onDelete = { pendingDelete = it },
            )
        }

        FloatingActionButton(
            onClick = {
                selectedUri = null
                selectedDocument = null
                editableName = ""
                selectedCategory = ""
                customCategory = ""
                source = VaultSource.Upload
                noteBody = ""
                isConfirmedToDismiss = false
                showBottomSheet = true
            },
            containerColor = Navy,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add document")
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showBottomSheet) {
        DocumentSaveBottomSheet(
            sheetState = sheetState,
            selectedDocument = selectedDocument,
            selectedUri = selectedUri,
            editableName = editableName,
            category = selectedCategory,
            customCategory = customCategory,
            onCustomCategoryChange = { customCategory = it },
            onCategoryChange = { selectedCategory = it },
            source = source,
            noteBody = noteBody,
            onNoteChange = { noteBody = it },
            onNameChange = { editableName = it },
            onPickFile = { source = VaultSource.Upload; documentPicker.launch(arrayOf("*/*")) },
            onScan = {
                source = VaultSource.Scan; cameraPermission.launch(Manifest.permission.CAMERA)
            },
            onSelectNote = {
                source = VaultSource.Note
                selectedUri = null
                selectedDocument = null
                if (editableName.isBlank()) editableName = "Secure Note"
            },
            onDismissRequest = { if (!isConfirmedToDismiss) showDiscardDialog = true },
            onSave = {
                val saved = saveCurrentDocument(
                    viewModel,
                    context,
                    source,
                    noteBody,
                    editableName,
                    resolvedCategory(),
                    selectedUri,
                    selectedDocument,
                )
                if (saved) scope.launch { snackbarHostState.showSnackbar("Document saved") }
                hideSheet()
            },
        )
    }

    if (showDiscardDialog) {
        DiscardDocumentDialog(
            onDismiss = { showDiscardDialog = false },
            onConfirm = { showDiscardDialog = false; hideSheet() },
        )
    }

    pendingDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Heart) },
            title = { Text("Delete document?") },
            text = { Text("\"${doc.name}\" will be removed from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDocument(doc.id)
                    pendingDelete = null
                    scope.launch { snackbarHostState.showSnackbar("Document deleted") }
                }) { Text("Delete", color = Heart) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}
