package com.example.sample.storage

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.IOException
import java.util.UUID


fun getDocumentInfo(context: Context, uri: Uri): SelectedDocument {
    var name = "Sample"
    var size: Long? = 0L

    context.contentResolver.query(
        uri,
        arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE
        ),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(
                OpenableColumns.DISPLAY_NAME
            )
            val sizeIndex = cursor.getColumnIndex(
                OpenableColumns.SIZE
            )
            if (nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                size = cursor.getLong(sizeIndex)
                    .takeIf { it >= 0L }
            }


        }
    }

    return SelectedDocument(
        uri.toString(),
        size,
        name,
        context.contentResolver.getType(uri)
    )
}

fun copyDocumentToInternalStorage(
    context: Context,
    uri: Uri
): String {
    val directory = File(
        context.filesDir,
        "vault/documents"
    )

    if (!directory.exists() && !directory.mkdirs()) {
        throw IOException("Could not create directory")
    }

    val fileName = UUID.randomUUID().toString()
    val destination = File(directory, fileName)

    val input = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Cannot open document")

    input.use { source ->
        destination.outputStream().use { output ->
            source.copyTo(output)
        }
    }

    // This is the relative path to store in Room.
    return "vault/documents/$fileName"
}