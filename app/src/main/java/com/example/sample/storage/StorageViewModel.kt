package com.example.sample.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.data.VaultDocumentImpl
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

sealed interface StorageUiState {
    data object Loading : StorageUiState
    data class Success(val documents: List<VaultDocumentEntity>) : StorageUiState
    data class Error(val message: String) : StorageUiState
}

@OptIn(FlowPreview::class)
class StorageViewModel(
    private val repository: VaultDocumentImpl,
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<StorageUiState> =
        combine(
            repository.getMyDocuments(),
            searchQuery
                .debounce(500.milliseconds)
                .distinctUntilChanged()
        ) { docs, query ->
            val filtered = if (query.isBlank()) {
                docs
            } else {
                docs.filter { document ->
                    document.name.contains(
                        query.trim(),
                        ignoreCase = true
                    )
                }
            }
            StorageUiState.Success(filtered) as StorageUiState
        }
            .catch { throwable ->
                emit(StorageUiState.Error(throwable.message ?: "Failed to load documents"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = StorageUiState.Loading
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
