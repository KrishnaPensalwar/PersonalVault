package com.example.sample.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.data.VaultDocumentImpl
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StorageViewModel(
    private val repository: VaultDocumentImpl
) : ViewModel() {

    val documents = repository.getAllDocuments()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun saveDocument(document: VaultDocumentEntity) {
        viewModelScope.launch {
            repository.saveDocument(document)
        }
    }
}
