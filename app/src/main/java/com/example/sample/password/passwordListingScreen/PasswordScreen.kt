package com.example.sample.password.passwordListingScreen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.example.sample.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.common.EmptyState
import com.example.sample.common.ErrorState
import com.example.sample.common.LoadingState
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.navigation.SetTopBar
import com.example.sample.password.PasswordCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

private val Navy = Color(0xFF1B2559)
private val Page = Color(0xFFF4F6FB)
private val Muted = Color(0xFF8B93A7)
private val Heart = Color(0xFFE11D48)

@Composable
fun PasswordScreen(
    viewModel: PasswordViewModel = koinViewModel(),
) {
    val uiState by viewModel.listUiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val backStack = LocalBackStack.current
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sortAz by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }

//    SetTopBar(titleContent = { FortressTopBarTitle() })

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is PasswordEvent.DeletedSuccessfully ->
                    snackbarHostState.showSnackbar("Password deleted")

                is PasswordEvent.Error ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when (val state = uiState) {
            is PasswordListUiState.Loading -> LoadingState(title = "Loading Passwords...")
            is PasswordListUiState.Error -> ErrorState(
                title = "Error Loading Passwords",
                description = state.message,
                image = Icons.Default.Lock,
            )

            is PasswordListUiState.Success -> {
                val shown = if (sortAz) {
                    state.passwords.sortedBy { it.title.lowercase() }
                } else {
                    state.passwords
                }
                if (state.passwords.isEmpty()) {
                    val currentFilter = filter
                    val emptyTitle = when (currentFilter) {
                        is PasswordListFilter.Favourites -> "No favourites yet"
                        is PasswordListFilter.Category -> "No ${currentFilter.name} passwords"
                        is PasswordListFilter.All -> "No passwords yet"
                    }
                    val emptyDesc = when (currentFilter) {
                        is PasswordListFilter.Favourites -> "Mark a password as favourite to see it here"
                        is PasswordListFilter.Category -> "No passwords saved under ${currentFilter.name}"
                        is PasswordListFilter.All -> "Tap + to add your first password"
                    }
                    EmptyState(
                        title = emptyTitle,
                        description = emptyDesc,
                        image = Icons.Default.Lock,
                    )

                }
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (false) item { VaultHeader() }

                    item {
                        FilterRow(
                            filter = filter,
                            total = state.totalCount,
                            favourites = state.favouriteCount,
                            categories = state.categoryCounts,
                            onSelect = viewModel::onFilterSelected,
                        )
                    }

                    if (shown.isNotEmpty()) {
                        item {
                            SearchField(
                                query = searchQuery,
                                total = state.totalCount,
                                onQueryChange = viewModel::onSearchQueryChanged,
                            )
                        }
                        item {
                            CredentialsHeader(
                                count = state.totalCount,
                                sortAz = sortAz,
                                onToggleSort = { sortAz = !sortAz },
                            )
                        }
                        items(items = shown, key = { it.id }) { password ->
                            PasswordCard(
                                password = password,
                                onClick = { backStack.add(Destinations.PasswordDetail(password.id)) },
                                onFavoriteClick = { viewModel.onToggleFavorite(password) },
                                onCopyClick = {
                                    copyPassword(context, password.password)
                                    scope.launch { snackbarHostState.showSnackbar("Password copied") }
                                },
                                onLongClick = { pendingDeleteId = password.id },
                            )
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { backStack.add(Destinations.PasswordForm()) },
            containerColor = Navy,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add password")
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Heart) },
            title = { Text("Delete Password?") },
            text = { Text("This action cannot be undone. Long-press a card to delete.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onDeletePassword(id)
                    pendingDeleteId = null
                }) { Text("Delete", color = Heart) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            },
        )
    }
}

private fun copyPassword(context: Context, secret: String) {
    com.example.sample.security.copySensitive(context, secret)
}

@Composable
private fun VaultHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE8EEFF)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF3D5AFE))
        }
        Spacer(Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Navy
                )
//                Spacer(Modifier.size(8.dp))
//                Text(
//                    "Encrypted",
//                    color = Color(0xFF1F8A4C),
//                    fontSize = 11.sp,
//                    fontWeight = FontWeight.SemiBold,
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(50))
//                        .background(Color(0xFFDDF6EA))
//                        .padding(horizontal = 8.dp, vertical = 2.dp),
//                )
            }
            Text("Passwords", color = Muted, fontSize = 13.sp)
        }
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            tint = Navy,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    total: Int,
    onQueryChange: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color.White,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Muted)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                textStyle = TextStyle(color = Navy, fontSize = 15.sp),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text("Search $total passwords, logins...", color = Muted, fontSize = 15.sp)
                    }
                    inner()
                },
            )
            if (false) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = Muted)
            }
        }
    }
}

@Composable
private fun FilterRow(
    filter: PasswordListFilter,
    total: Int,
    favourites: Int,
    categories: List<CategoryCount>,
    onSelect: (PasswordListFilter) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            if (categories.size > 1) {
                CountChip(
                    label = "All",
                    count = total,
                    selected = filter == PasswordListFilter.All,
                    icon = null,
                    onClick = { onSelect(PasswordListFilter.All) },
                )
            }
        }
        if (favourites > 0) {
            item {
                CountChip(
                    label = "Favourites",
                    count = favourites,
                    selected = filter == PasswordListFilter.Favourites,
                    icon = Icons.Default.Favorite,
                    onClick = { onSelect(PasswordListFilter.Favourites) },
                )
            }
        }
        items(categories, key = { it.name }) { cat ->
            if (categories.size > 1) {
                val icon = when {
                    cat.name.equals("Work", ignoreCase = true) -> Icons.Default.Work
                    cat.name.equals("Personal", ignoreCase = true) -> Icons.Default.Person
                    else -> null
                }
                val isSelected = filter is PasswordListFilter.Category && filter.name.equals(
                    cat.name,
                    ignoreCase = true
                )
                CountChip(
                    label = cat.name,
                    count = cat.count,
                    selected = isSelected,
                    icon = icon,
                    onClick = { onSelect(PasswordListFilter.Category(cat.name)) },
                )
            }
        }
    }
}

@Composable
private fun CountChip(
    label: String,
    count: Int,
    selected: Boolean,
    icon: ImageVector?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) Navy else Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else if (label == "Favourites") Heart else Navy,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.size(4.dp))
            }
            Text(
                "$label $count",
                color = if (selected) Color.White else Navy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun CredentialsHeader(count: Int, sortAz: Boolean, onToggleSort: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Credentials", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
        Spacer(Modifier.size(8.dp))
        Text(
            "Total $count",
            color = Color(0xFF3D5AFE),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE8EEFF))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(Modifier.weight(1f))
        if (false) {
            IconButton(onClick = onToggleSort) {
                Icon(
                    Icons.Default.SortByAlpha,
                    contentDescription = "Sort A-Z",
                    tint = if (sortAz) Navy else Muted,
                )
            }
        }
        Surface(
            onClick = onToggleSort,
            shape = RoundedCornerShape(50),
            color = if (sortAz) Navy else Color.White,
        ) {
            Text(
                "A–Z",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (sortAz) Color.White else Navy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}
