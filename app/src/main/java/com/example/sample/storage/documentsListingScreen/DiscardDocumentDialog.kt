package com.example.sample.storage.documentsListingScreen

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun DiscardDocumentDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard Document?") },
        text = { Text("Are you sure you want to discard this document? It won't be saved.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Discard") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
