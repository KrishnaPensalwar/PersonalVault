package com.example.sample.family

import com.example.sample.family.data.FamilyMemberEntity

// ── UI State ──────────────────────────────────────────────────────────────────

sealed interface FamilyUiState {
    data object Loading : FamilyUiState
    data class Success(val members: List<FamilyMemberEntity>) : FamilyUiState
    data class Error(val message: String) : FamilyUiState
}

// ── One-shot events ───────────────────────────────────────────────────────────

sealed interface FamilyEvent {
    data object SavedSuccessfully : FamilyEvent
    data object DeletedSuccessfully : FamilyEvent
    data class Error(val message: String) : FamilyEvent
}
