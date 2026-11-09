package com.example.sample.family.membersListingScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.sample.navigation.SetTopBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.FamilyRestroom
import com.example.sample.common.EmptyState
import com.example.sample.common.ErrorState
import com.example.sample.common.LoadingState
import com.example.sample.family.FamilyMemberCard
import com.example.sample.family.data.FamilyMemberEntity
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val Navy = Color(0xFF1B2559)
private val Page = Color(0xFFF4F6FB)
private val Heart = Color(0xFFE11D48)

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
    var memberQuery by remember { mutableStateOf("") }
    var memberFormDirty by remember { mutableStateOf(false) }
    var showDiscardMember by remember { mutableStateOf(false) }

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

//    SetTopBar(titleContent = {
//        Text("Fortress", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Navy)
//    })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val state = uiState) {
                is FamilyUiState.Loading -> {
                    LoadingState(title = "Loading Family Members...")
                }

                is FamilyUiState.Error -> {
                    ErrorState(
                        title = "Error Loading Family",
                        description = state.message,
                        image = Icons.Default.FamilyRestroom
                    )
                }

                is FamilyUiState.Success -> {
                    val shown = state.members.filter { member ->
                        val q = memberQuery.trim()
                        q.isEmpty() || member.name.contains(q, true) || member.relationship.contains(q, true)
                    }
                    if (state.members.isEmpty()) {
                        EmptyState(
                            title = "No family members yet",
                            description = "Tap + to add your first family member",
                            image = Icons.Default.FamilyRestroom
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            item {
                                val scheme = MaterialTheme.colorScheme
                                Surface(shape = RoundedCornerShape(28.dp), color = scheme.surface, modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Default.Search, contentDescription = null, tint = scheme.onSurfaceVariant)
                                        BasicTextField(
                                            value = memberQuery,
                                            onValueChange = { memberQuery = it },
                                            singleLine = true,
                                            textStyle = TextStyle(color = scheme.onSurface, fontSize = 15.sp),
                                            modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp),
                                            decorationBox = { inner ->
                                                if (memberQuery.isEmpty()) {
                                                    Text("Search family members", color = scheme.onSurfaceVariant, fontSize = 15.sp)
                                                }
                                                inner()
                                            },
                                        )
                                        if (memberQuery.isNotEmpty()) {
                                            IconButton(onClick = { memberQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear search", tint = scheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
//                            item {
//                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
//                                    Text("Family", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
//                                    Spacer(Modifier.size(8.dp))
//                                    Text(
//                                        "${state.members.size}",
//                                        color = Color(0xFF3D5AFE),
//                                        fontSize = 12.sp,
//                                        fontWeight = FontWeight.SemiBold,
//                                        modifier = Modifier
//                                            .clip(RoundedCornerShape(50))
//                                            .background(Color(0xFFE8EEFF))
//                                            .padding(horizontal = 8.dp, vertical = 2.dp),
//                                    )
//                                }
//                            }
                            if (shown.isEmpty()) {
                                item {
                                    EmptyState(
                                        title = "No matching members",
                                        description = "Try a different name or relationship",
                                        image = Icons.Default.FamilyRestroom,
                                    )
                                }
                            }
                            items(shown, key = { it.id }) { member ->
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

        FloatingActionButton(
            onClick = {
                editingMember = null
                showSheet = true
            },
            containerColor = Navy,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add member")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // ── Add / Edit bottom sheet ───────────────────────────────────────────────

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                if (memberFormDirty) showDiscardMember = true else {
                    showSheet = false
                    editingMember = null
                }
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
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
                onDirtyChange = { memberFormDirty = it },
                onCancel = {
                    if (memberFormDirty) showDiscardMember = true
                    else scope.launch { sheetState.hide() }.invokeOnCompletion {
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
                    tint = Heart,
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
                    Text("Delete", color = Heart)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMember = null }) { Text("Cancel") }
            },
        )
    }

    if (showDiscardMember) {
        AlertDialog(
            onDismissRequest = { showDiscardMember = false },
            title = { Text("Discard member?") },
            text = { Text("Name and relationship will not be saved.") },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardMember = false
                    memberFormDirty = false
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        editingMember = null
                    }
                }) { Text("Discard", color = Heart) }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardMember = false }) { Text("Keep editing") }
            },
        )
    }
}