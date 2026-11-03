package com.example.sample.storage.documentsListingScreen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri

fun loadDocumentPreviewFromUri(
    context: Context,
    uri: String,
    mimeType: String?,
): Bitmap? {
    return try {
        when {
            mimeType?.startsWith("image/") == true -> {
                context.contentResolver.openInputStream(uri.toUri())?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }

            mimeType == "application/pdf" -> {
                val fd = context.contentResolver.openFileDescriptor(uri.toUri(), "r")
                fd?.use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        if (renderer.pageCount == 0) return null
                        renderer.openPage(0).use { page ->
                            val targetWidth = 400
                            val scale = targetWidth.toFloat() / page.width
                            val targetHeight = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = createBitmap(targetWidth, targetHeight)
                            bitmap.eraseColor(AndroidColor.WHITE)
                            page.render(
                                bitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY,
                            )
                            return bitmap
                        }
                    }
                }
            }

            else -> null
        }
    } catch (_: Exception) {
        null
    }
}
