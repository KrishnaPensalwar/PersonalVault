package com.example.sample.storage.documentDetailScreen

import com.example.sample.storage.data.VaultDocumentEntity

sealed interface DocumentDetailUiState {
    data object Loading : DocumentDetailUiState
    data class Success(val document: VaultDocumentEntity) : DocumentDetailUiState
    data class Error(val message: String) : DocumentDetailUiState
}
