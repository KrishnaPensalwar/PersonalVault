package com.example.sample.family.memberProfile

import com.example.sample.family.data.FamilyMemberEntity
import com.example.sample.storage.data.VaultDocumentEntity

// ── UI State ──────────────────────────────────────────────────────────────────

data class MemberProfileUiState(
    val member: FamilyMemberEntity? = null,
    val documents: List<VaultDocumentEntity> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)


sealed interface MemberProfileEvent {
    data object DeletedSuccessfully : MemberProfileEvent
    data class Error(val message: String) : MemberProfileEvent
}