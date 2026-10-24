package com.example.sample.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.data.VaultDocumentImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DocumentDetailViewModel(
    private val repository: VaultDocumentImpl
) : ViewModel() {

    private val _documentState = MutableStateFlow<VaultDocumentEntity?>(null)
    val documentState: StateFlow<VaultDocumentEntity?> = _documentState.asStateFlow()

    fun loadDocument(id: String) {
        viewModelScope.launch {
            _documentState.value = repository.getDocumentById(id)
        }
    }

    fun updateDocumentName(id: String, newName: String) {
        viewModelScope.launch {
            repository.updateDocumentDetails(id, newName)
            loadDocument(id)
        }
    }

    fun deleteDocument(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteDocumentById(id)
            onSuccess()
        }
    }
}
