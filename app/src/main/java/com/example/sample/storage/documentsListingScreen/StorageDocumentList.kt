package com.example.sample.storage.documentsListingScreen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sample.common.EmptyState
import com.example.sample.navigation.Destinations
import com.example.sample.storage.StorageDocumentCard
import com.example.sample.storage.data.VaultDocumentEntity

@Composable
internal fun StorageDocumentList(
    documents: List<VaultDocumentEntity>,
    searchQuery: String,
    categoryFilter: String,
    sortAz: Boolean,
    onCategoryFilter: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onToggleSort: () -> Unit,
    onOpen: (String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (VaultDocumentEntity) -> Unit,
) {
    val categoryChips = documents.map { it.category.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
    LaunchedEffect(categoryChips) {
        if (categoryFilter != "All Files" && categoryFilter !in categoryChips) onCategoryFilter("All Files")
    }
    val filtered = documents
        .filter { categoryFilter == "All Files" || it.category == categoryFilter }
        .let { list -> if (sortAz) list.sortedBy { it.name.lowercase() } else list }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            StorageSearchField(searchQuery, onSearchChange, onClearSearch)
        }
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StorageChip("All Files ${documents.size}", categoryFilter == "All Files") {
                    onCategoryFilter("All Files")
                }
                categoryChips.forEach { chip ->
                    StorageChip("$chip ${documents.count { it.category == chip }}", categoryFilter == chip) {
                        onCategoryFilter(chip)
                    }
                }
            }
        }
//        item { UsageCard(documents) }
        item { RecordsHeader(filtered.size, sortAz, onToggleSort) }
        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    title = if (searchQuery.isBlank() && categoryFilter == "All Files") {
                        "No documents saved yet"
                    } else {
                        "No matching documents"
                    },
                    description = "Tap + to add a document",
                    image = Icons.Default.Storage,
                )
            }
        } else {
            items(filtered, key = { it.id }) { doc ->
                StorageDocumentCard(
                    document = doc,
                    onClick = { onOpen(doc.id) },
                    onEditClick = { onEdit(doc.id) },
                    onDeleteClick = { onDelete(doc) },
                )
            }
        }
    }
}
