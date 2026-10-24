package com.example.sample.navigation

import androidx.navigation3.runtime.NavKey
import com.example.sample.storage.SelectedDocument
import com.example.sample.storage.data.VaultDocumentEntity
import kotlinx.serialization.Serializable

@Serializable
sealed interface Destinations : NavKey{

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
    data class DocumentDetailScreen(val documentEntity: SelectedDocument ?= null) : Destinations{
        override val showBottomBar: Boolean
            get() = false
    }


}