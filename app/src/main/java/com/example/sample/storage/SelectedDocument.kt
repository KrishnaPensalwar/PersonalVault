package com.example.sample.storage

import android.net.Uri


data class SelectedDocument(
    val uri: Uri,
    val size: Long?,
    val fileName: String,
    val mimeType: String?
)