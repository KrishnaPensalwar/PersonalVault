package com.example.sample.family.memberProfile

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import com.example.sample.common.AppMessages
import com.example.sample.common.EmptyState
import com.example.sample.common.ErrorState
import com.example.sample.common.LoadingState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import com.example.sample.navigation.SetTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.family.MemberInfoCard
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.storage.documentsListingScreen.DocumentSaveBottomSheet
import com.example.sample.storage.SelectedDocument
import com.example.sample.storage.StorageDocumentCard
import com.example.sample.storage.copyDocumentToInternalStorage
import com.example.sample.storage.documentsListingScreen.VaultSource
import com.example.sample.storage.writeTextDocument
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.getDocumentInfo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.io.File
import java.util.UUID

private val Navy = Color(0xFF1B2559)
private val Page = Color(0xFFF4F6FB)
private val Heart = Color(0xFFE11D48)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyMemberProfileScreen(
    memberId: String,
    viewModel: FamilyMemberProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backStack = LocalBackStack.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Document picker state
    var selectedDocument by remember { mutableStateOf<SelectedDocument?>(null) }
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var editableName by remember { mutableStateOf("") }
    var showDocBottomSheet by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isConfirmedToDismiss by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf(VaultSource.Upload) }
    var noteBody by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var customCategory by remember { mutableStateOf("") }
    var pendingScanUri by remember { mutableStateOf<Uri?>(null) }

    // Delete document confirmation
    var pendingDeleteDocId by remember { mutableStateOf<String?>(null) }

    val docSheetState = rememberModalBottomSheetState(
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

    LaunchedEffect(memberId) {
        viewModel.loadMember(memberId)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is MemberProfileEvent.DeletedSuccessfully -> {
                    AppMessages.show("Member deleted")
                    backStack.removeLastOrNull()
                }
                is MemberProfileEvent.DocumentSaved -> snackbarHostState.showSnackbar("Document saved")
                is MemberProfileEvent.DocumentDeleted -> snackbarHostState.showSnackbar("Document deleted")
                is MemberProfileEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    fun resolvedCategory(): String =
        if (selectedCategory == "Other") customCategory.trim() else selectedCategory.trim()

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = pendingScanUri
        if (saved && uri != null) {
            val name = "Scan_${System.currentTimeMillis()}.jpg"
            val length = runCatching {
                context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
            }.getOrNull() ?: 0L
            selectedUri = uri
            selectedDocument = SelectedDocument(uri.toString(), length, name, "image/jpeg")
            editableName = name
            source = VaultSource.Scan
        }
    }

    fun launchScan() {
        val file = File(context.cacheDir, "scan_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingScanUri = uri
        takePicture.launch(uri)
    }

    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchScan()
        else scope.launch { snackbarHostState.showSnackbar("Camera permission needed to scan") }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val docInfo = getDocumentInfo(context, it)
            selectedDocument = docInfo
            selectedUri = it
            editableName = docInfo.fileName
            source = VaultSource.Upload
            isConfirmedToDismiss = false
            showDocBottomSheet = true
        }
    }

    SetTopBar(
        title = uiState.member?.name ?: "Member Profile"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            uiState.isLoading -> {
                LoadingState(title = "Loading Member Profile...")
            }

            uiState.error != null -> {
                ErrorState(
                    title = "Error Loading Profile",
                    description = uiState.error ?: "Member profile could not be loaded",
                    image = Icons.Default.FamilyRestroom
                )
            }

            uiState.member != null -> {
                MemberProfileContent(
                    uiState = uiState,
                    onDocumentClick = { docId ->
                        backStack.add(Destinations.DocumentViewer(id = docId))
                    },
                    onEditDocument = { docId ->
                        backStack.add(Destinations.DocumentDetailScreen(id = docId))
                    },
                    onDeleteDocument = { id -> pendingDeleteDocId = id },
                )
            }
        }

        FloatingActionButton(
            onClick = {
                selectedUri = null
                selectedDocument = null
                editableName = ""
                source = VaultSource.Upload
                noteBody = ""
                selectedCategory = ""
                customCategory = ""
                isConfirmedToDismiss = false
                showDocBottomSheet = true
            },
            containerColor = Navy,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add document")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // ── Document save bottom sheet ────────────────────────────────────────────

    if (showDocBottomSheet) {
        DocumentSaveBottomSheet(
            sheetState = docSheetState,
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
            onPickFile = {
                source = VaultSource.Upload
                documentPicker.launch(arrayOf("*/*"))
            },
            onScan = {
                source = VaultSource.Scan
                cameraPermission.launch(Manifest.permission.CAMERA)
            },
            onSelectNote = {
                source = VaultSource.Note
                selectedUri = null
                selectedDocument = null
                if (editableName.isBlank()) editableName = "Secure Note"
            },
            onDismissRequest = {
                if (!isConfirmedToDismiss) showDiscardDialog = true
            },
            onSave = {
                val category = resolvedCategory()
                when (source) {
                    VaultSource.Note -> {
                        val text = noteBody.trim()
                        if (text.isNotEmpty()) {
                            viewModel.saveDocument(
                                VaultDocumentEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = editableName.ifBlank { "Secure Note" },
                                    relativePath = writeTextDocument(context, text),
                                    mimeType = "text/plain",
                                    sizeBytes = text.toByteArray().size.toLong(),
                                    familyMemberId = memberId,
                                    category = category,
                                )
                            )
                        }
                    }
                    else -> {
                        val uri = selectedUri
                        val doc = selectedDocument
                        if (uri != null && doc != null) {
                            viewModel.saveDocument(
                                VaultDocumentEntity(
                                    id = UUID.randomUUID().toString(),
                                    name = editableName.ifBlank { doc.fileName },
                                    relativePath = copyDocumentToInternalStorage(context, uri),
                                    mimeType = doc.mimeType,
                                    sizeBytes = doc.size ?: 0L,
                                    familyMemberId = memberId,
                                    category = category,
                                )
                            )
                        }
                    }
                }
                isConfirmedToDismiss = true
                scope.launch {
                    docSheetState.hide()
                }.invokeOnCompletion {
                    showDocBottomSheet = false
                    selectedUri = null
                    selectedDocument = null
                }
            }
        )
    }

    // ── Discard document dialog ───────────────────────────────────────────────

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Discard Document?") },
            text = { Text("Are you sure you want to discard this document? It won't be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardDialog = false
                    isConfirmedToDismiss = true
                    scope.launch { docSheetState.hide() }.invokeOnCompletion {
                        showDocBottomSheet = false
                        selectedUri = null
                        selectedDocument = null
                    }
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Delete document confirmation ──────────────────────────────────────────

    pendingDeleteDocId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteDocId = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = Heart,
                )
            },
            title = { Text("Delete Document?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDocument(id)
                    pendingDeleteDocId = null
                }) {
                    Text("Delete", color = Heart)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteDocId = null }) { Text("Cancel") }
            },
        )
    }
}

// ── Profile content ───────────────────────────────────────────────────────────

@Composable
private fun MemberProfileContent(
    uiState: MemberProfileUiState,
    onDocumentClick: (String) -> Unit,
    onEditDocument: (String) -> Unit,
    onDeleteDocument: (String) -> Unit,
) {
    val member = uiState.member ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Member info header card ───────────────────────────────────────────
        item(key = "member_info") {
            MemberInfoCard(member = member)
        }

        // ── Documents section header ──────────────────────────────────────────
        item(key = "docs_header") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Text("Documents", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
                Spacer(Modifier.size(8.dp))
                Text(
                    "${uiState.documents.size}",
                    color = Color(0xFF3D5AFE),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE8EEFF))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }

        if (uiState.documents.isEmpty()) {
            item(key = "docs_empty") {
                EmptyState(
                    title = "No documents yet",
                    description = "Tap + to add a document for ${member.name}",
                    image = Icons.AutoMirrored.Filled.InsertDriveFile,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                )
            }
        } else {
            items(uiState.documents, key = { it.id }) { doc ->
                StorageDocumentCard(
                    document = doc,
                    onClick = { onDocumentClick(doc.id) },
                    onEditClick = { onEditDocument(doc.id) },
                    onDeleteClick = { onDeleteDocument(doc.id) },
                )
            }
        }

        // Bottom spacer so FAB doesn't overlap last item
        item(key = "spacer_bottom") { Spacer(Modifier.height(80.dp)) }
    }
}
