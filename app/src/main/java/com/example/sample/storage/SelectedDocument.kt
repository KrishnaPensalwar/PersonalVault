package com.example.sample.storage

import kotlinx.serialization.Serializable


@Serializable
data class SelectedDocument(
    val uri: String,
    val size: Long?,
    val fileName: String,
    val mimeType: String?
)