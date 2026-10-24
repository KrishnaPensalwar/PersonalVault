package com.example.sample.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.data.VaultDocumentImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DocumentDetailUiState {
    data object Loading : DocumentDetailUiState
    data class Success(val document: VaultDocumentEntity) : DocumentDetailUiState
    data class Error(val message: String) : DocumentDetailUiState
}

class DocumentDetailViewModel(
    private val repository: VaultDocumentImpl
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentDetailUiState>(DocumentDetailUiState.Loading)
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    fun loadDocument(id: String) {
        viewModelScope.launch {
            _uiState.value = DocumentDetailUiState.Loading
            try {
                val document = repository.getDocumentById(id)
                if (document != null) {
                    _uiState.value = DocumentDetailUiState.Success(document)
                } else {
                    _uiState.value = DocumentDetailUiState.Error("Document not found")
                }
            } catch (e: Exception) {
                _uiState.value = DocumentDetailUiState.Error(e.message ?: "Failed to load document")
            }
        }
    }

    fun updateDocumentName(id: String, newName: String) {
        viewModelScope.launch {
            try {
                repository.updateDocumentDetails(id, newName)
                loadDocument(id)
            } catch (e: Exception) {
                _uiState.value = DocumentDetailUiState.Error(e.message ?: "Failed to update document")
            }
        }
    }

    fun deleteDocument(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                repository.deleteDocumentById(id)
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = DocumentDetailUiState.Error(e.message ?: "Failed to delete document")
            }
        }
    }
}
