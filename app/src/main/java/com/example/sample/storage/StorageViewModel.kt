package com.example.sample.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.data.VaultDocumentImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class StorageViewModel(
    private val repository: VaultDocumentImpl
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val documents: StateFlow<List<VaultDocumentEntity>> =
        combine(
            repository.getAllDocuments(),
            searchQuery
                .debounce(500.milliseconds)
                .distinctUntilChanged()
        ) { docs, query ->
            if (query.isBlank()) {
                docs
            } else {
                docs.filter { document ->
                    document.name.contains(
                        query.trim(),
                        ignoreCase = true
                    )
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun getDocuments(query: String = "") {
        searchQuery.value = query
    }

    fun saveDocument(document: VaultDocumentEntity) {
        viewModelScope.launch {
            repository.saveDocument(document)
        }
    }
}
