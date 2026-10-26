package com.example.sample.family.membersListingScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.family.EmptyState
import com.example.sample.family.FamilyMemberCard
import com.example.sample.family.data.FamilyMemberEntity
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(viewModel: FamilyViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val documentCounts by viewModel.documentCounts.collectAsStateWithLifecycle()
    val backStack = LocalBackStack.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Bottom sheet state for add/edit member form
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<FamilyMemberEntity?>(null) }

    // Delete confirmation
    var pendingDeleteMember by remember { mutableStateOf<FamilyMemberEntity?>(null) }

    // Start observing document counts whenever the list updates
    val members = (uiState as? FamilyUiState.Success)?.members ?: emptyList()
    LaunchedEffect(members) {
        members.forEach { member ->
            if (!documentCounts.containsKey(member.id)) {
                viewModel.observeDocumentCountForMember(member.id)
            }
        }
    }

    // Collect one-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is FamilyEvent.DeletedSuccessfully ->
                    snackbarHostState.showSnackbar("Member deleted")
                is FamilyEvent.SavedSuccessfully ->
                    snackbarHostState.showSnackbar("Member saved")
                is FamilyEvent.Error ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Family",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingMember = null
                    showSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add member")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is FamilyUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading…")
                    }
                }

                is FamilyUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                is FamilyUiState.Success -> {
                    if (state.members.isEmpty()) {
                        EmptyState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.members, key = { it.id }) { member ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(tween(300)) + slideInVertically(tween(300)),
                                ) {
                                    FamilyMemberCard(
                                        member = member,
                                        documentCount = documentCounts[member.id] ?: 0,
                                        onClick = {
                                            backStack.add(Destinations.FamilyMemberProfile(member.id))
                                        },
                                        onEditClick = {
                                            editingMember = member
                                            showSheet = true
                                        },
                                        onDeleteClick = {
                                            pendingDeleteMember = member
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Add / Edit bottom sheet ───────────────────────────────────────────────

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showSheet = false
                editingMember = null
            },
            sheetState = sheetState,
        ) {
            MemberFormContent(
                initial = editingMember,
                onSave = { name, relationship ->
                    val existing = editingMember
                    if (existing == null) {
                        viewModel.saveMember(name, relationship)
                    } else {
                        viewModel.updateMember(existing, name, relationship)
                    }
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        editingMember = null
                    }
                },
                onCancel = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        editingMember = null
                    }
                },
            )
        }
    }

    // ── Delete confirmation ───────────────────────────────────────────────────

    pendingDeleteMember?.let { member ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMember = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text("Delete Member?") },
            text = {
                Text(
                    "All documents belonging to ${member.name} will also be deleted. " +
                            "This action cannot be undone."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMember(member.id)
                    pendingDeleteMember = null
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMember = null }) { Text("Cancel") }
            },
        )
    }
}