package com.example.sample.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.family.data.FamilyMemberEntity
import com.example.sample.family.data.FamilyMemberRepository
import com.example.sample.storage.data.VaultDocumentImpl
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class FamilyViewModel(
    private val repository: FamilyMemberRepository,
    private val documentRepository: VaultDocumentImpl,
) : ViewModel() {

    val uiState: StateFlow<FamilyUiState> = repository.getAllMembers()
        .map { members -> FamilyUiState.Success(members) as FamilyUiState }
        .catch { emit(FamilyUiState.Error(it.message ?: "Failed to load family members")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = FamilyUiState.Loading,
        )

    private val _events = MutableSharedFlow<FamilyEvent>()
    val events = _events.asSharedFlow()

    // ── Document count cache per member (live) ────────────────────────────────

    private val _documentCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val documentCounts: StateFlow<Map<String, Int>> = _documentCounts

    fun observeDocumentCountForMember(memberId: String) {
        viewModelScope.launch {
            documentRepository.getDocumentCountByMemberId(memberId)
                .collect { count ->
                    _documentCounts.value += (memberId to count)
                }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────
    fun saveMember(name: String, relationship: String) {
        viewModelScope.launch {
            runCatching {
                repository.saveMember(
                    FamilyMemberEntity(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        relationship = relationship.trim(),
                    )
                )
                _events.emit(FamilyEvent.SavedSuccessfully)
            }.onFailure {
                _events.emit(FamilyEvent.Error(it.message ?: "Failed to save member"))
            }
        }
    }

    fun updateMember(entity: FamilyMemberEntity, name: String, relationship: String) {
        viewModelScope.launch {
            runCatching {
                repository.updateMember(
                    entity.copy(
                        name = name.trim(),
                        relationship = relationship.trim(),
                        updatedAt = System.currentTimeMillis(),
                    )
                )
                _events.emit(FamilyEvent.SavedSuccessfully)
            }.onFailure {
                _events.emit(FamilyEvent.Error(it.message ?: "Failed to update member"))
            }
        }
    }

    fun deleteMember(id: String) {
        viewModelScope.launch {
            runCatching {
                // Delete all documents linked to this member first
                documentRepository.deleteDocumentsByMemberId(id)
                repository.deleteMemberById(id)
                _events.emit(FamilyEvent.DeletedSuccessfully)
            }.onFailure {
                _events.emit(FamilyEvent.Error(it.message ?: "Failed to delete member"))
            }
        }
    }
}
