package com.example.sample.family

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.family.data.FamilyMemberEntity
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.storage.DocumentSaveBottomSheet
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.member?.name ?: "Member Profile",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
                windowInsets = WindowInsets(0, 0, 0, 0)

            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { documentPicker.launch(arrayOf("*/*")) },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add document")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center),
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
        }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "No documents yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Tap + to add a document for ${member.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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

@Composable
private fun MemberInfoCard(member: FamilyMemberEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Large initial avatar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = member.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Spacer(Modifier.width(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = member.relationship,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                )
            }
        }
    }
}
