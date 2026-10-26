package com.example.sample.family.memberProfile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.family.MemberInfoCard
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.storage.documentsListingScreen.DocumentSaveBottomSheet
import com.example.sample.storage.SelectedDocument
import com.example.sample.storage.StorageDocumentCard
import com.example.sample.storage.copyDocumentToInternalStorage
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.getDocumentInfo
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import java.util.UUID

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
                is MemberProfileEvent.DeletedSuccessfully -> backStack.removeLastOrNull()
                is MemberProfileEvent.Error -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val docInfo = getDocumentInfo(context, it)
            selectedDocument = docInfo
            selectedUri = it
            editableName = docInfo.fileName
            isConfirmedToDismiss = false
            showDocBottomSheet = true
        }
    }

    SetTopBar(
        title = uiState.member?.name ?: "Member Profile"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
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
            onClick = { documentPicker.launch(arrayOf("*/*")) },
            containerColor = MaterialTheme.colorScheme.primary,
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
            onNameChange = { editableName = it },
            onDismissRequest = {
                if (!isConfirmedToDismiss) showDiscardDialog = true
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
                            sizeBytes = doc.size ?: 0L,
                            familyMemberId = memberId,
                        )
                    )
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
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Delete Document?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDocument(id)
                    pendingDeleteDocId = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
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
            Text(
                text = "Documents (${uiState.documents.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
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
