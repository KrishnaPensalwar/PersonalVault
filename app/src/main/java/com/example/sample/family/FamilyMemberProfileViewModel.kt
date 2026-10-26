package com.example.sample.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sample.family.data.FamilyMemberEntity
import com.example.sample.family.data.FamilyMemberRepository
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.data.VaultDocumentImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

data class MemberProfileUiState(
    val member: FamilyMemberEntity? = null,
    val documents: List<VaultDocumentEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

// ── One-shot events ───────────────────────────────────────────────────────────

sealed interface MemberProfileEvent {
    data object DeletedSuccessfully : MemberProfileEvent
    data class Error(val message: String) : MemberProfileEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyMemberProfileViewModel(
    private val memberRepository: FamilyMemberRepository,
    private val documentRepository: VaultDocumentImpl,
) : ViewModel() {

    private val _memberId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<MemberProfileUiState> =
        _memberId.flatMapLatest { id ->
            if (id == null) {
                flowOf(MemberProfileUiState(isLoading = true))
            } else {
                combine(
                    memberRepository.getAllMembers().map { list -> list.find { it.id == id } },
                    documentRepository.getDocumentsByMemberId(id),
                ) { member, docs ->
                    if (member == null) {
                        MemberProfileUiState(isLoading = false, error = "Member not found")
                    } else {
                        MemberProfileUiState(member = member, documents = docs, isLoading = false)
                    }
                }
            }
        }
            .catch { emit(MemberProfileUiState(isLoading = false, error = it.message ?: "Error")) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = MemberProfileUiState(isLoading = true),
            )

    private val _events = MutableSharedFlow<MemberProfileEvent>()
    val events = _events.asSharedFlow()

    fun loadMember(memberId: String) {
        _memberId.value = memberId
    }

    fun saveDocument(document: VaultDocumentEntity) {
        viewModelScope.launch {
            runCatching {
                documentRepository.saveDocument(document)
            }.onFailure {
                _events.emit(MemberProfileEvent.Error(it.message ?: "Failed to save document"))
            }
        }
    }

    fun deleteDocument(id: String) {
        viewModelScope.launch {
            runCatching {
                documentRepository.deleteDocumentById(id)
            }.onFailure {
                _events.emit(MemberProfileEvent.Error(it.message ?: "Failed to delete document"))
            }
        }
    }
}
