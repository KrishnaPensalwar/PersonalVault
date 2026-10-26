package com.example.sample.storage.documentsListingScreen

import com.example.sample.storage.data.VaultDocumentEntity

sealed interface StorageUiState {
    data object Loading : StorageUiState
    data class Success(val documents: List<VaultDocumentEntity>) : StorageUiState
    data class Error(val message: String) : StorageUiState
}
