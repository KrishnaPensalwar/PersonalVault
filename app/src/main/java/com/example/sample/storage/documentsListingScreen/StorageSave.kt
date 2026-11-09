package com.example.sample.storage.documentsListingScreen

import android.content.Context
import android.net.Uri
import com.example.sample.storage.SelectedDocument
import com.example.sample.storage.copyDocumentToInternalStorage
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.writeTextDocument
import java.util.UUID

internal fun saveCurrentDocument(
    viewModel: StorageViewModel,
    context: Context,
    source: VaultSource,
    noteBody: String,
    editableName: String,
    category: String,
    selectedUri: Uri?,
    selectedDocument: SelectedDocument?,
): Boolean {
    if (source == VaultSource.Note) {
        val text = noteBody.trim()
        if (text.isEmpty()) return false
        viewModel.saveDocument(
            VaultDocumentEntity(
                id = UUID.randomUUID().toString(),
                name = editableName.ifBlank { "Secure Note" },
                relativePath = writeTextDocument(context, text),
                mimeType = "text/plain",
                sizeBytes = text.toByteArray().size.toLong(),
                category = category,
            ),
        )
        return true
    }
    val uri = selectedUri ?: return false
    val doc = selectedDocument ?: return false
    viewModel.saveDocument(
        VaultDocumentEntity(
            id = UUID.randomUUID().toString(),
            name = editableName.ifBlank { doc.fileName },
            relativePath = copyDocumentToInternalStorage(context, uri),
            mimeType = doc.mimeType,
            sizeBytes = doc.size ?: 0L,
            category = category,
        ),
    )
    return true
}
