package com.example.sample.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destinations : NavKey {

    val showBottomBar: Boolean
        get() = true

    @Serializable
    data object Dashboard : Destinations

    @Serializable
    data object Storage : Destinations

    @Serializable
    data object Password : Destinations

    @Serializable
    data object Family : Destinations

    @Serializable
    data class DocumentDetailScreen(val id: String) : Destinations {
        override val showBottomBar: Boolean
            get() = false
    }

    @Serializable
    data class DocumentViewer(val id: String) : Destinations {
        override val showBottomBar: Boolean
            get() = false
    }

    // ── Password sub-screens ──────────────────────────────────────────────────

    /**
     * Add (passwordId == null) or Edit (passwordId != null) form.
     */
    @Serializable
    data class PasswordForm(val passwordId: String? = null) : Destinations {
        override val showBottomBar: Boolean
            get() = false
    }

    @Serializable
    data class PasswordDetail(val passwordId: String) : Destinations {
        override val showBottomBar: Boolean
            get() = false
    }

    // ── Family sub-screens ────────────────────────────────────────────────────

    /**
     * Profile screen for an individual family member.
     */
    @Serializable
    data class FamilyMemberProfile(val memberId: String) : Destinations {
        override val showBottomBar: Boolean
            get() = false
    }
}