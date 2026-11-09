package com.example.sample.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.example.sample.storage.data.VaultDocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

private val Ink = Color(0xFF1B2559)
private val Muted = Color(0xFF8B93A7)
private val Body = Color(0xFF5C6578)

@Composable
fun StorageDocumentCard(
    document: VaultDocumentEntity,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val preview by produceState<Bitmap?>(
        initialValue = null,
        key1 = document.relativePath,
        key2 = document.mimeType
    ) {
        value = withContext(Dispatchers.IO) {
            loadDocumentPreview(context, document)
        }
    }

    val kind = fileKind(document.mimeType)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {},
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(kind.wash),
                contentAlignment = Alignment.Center,
            ) {
                if (preview != null) {
                    Image(
                        bitmap = preview!!.asImageBitmap(),
                        contentDescription = "${document.name} preview",
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(kind.icon, contentDescription = null, tint = kind.tint, modifier = Modifier.size(20.dp))
                        Text(kind.label, color = kind.tint, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.name,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metaLine(document),
                    color = Body,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Muted, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Private", color = Muted, fontSize = 12.sp)
                }
            }

            if (onEditClick != null) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit document", tint = Color(0xFF9AA3B5), modifier = Modifier.size(18.dp))
                }
            }
            if (onDeleteClick != null) {
                IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete document", tint = Color(0xFFE11D48), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private data class FileKind(val label: String, val icon: ImageVector, val tint: Color, val wash: Color)

private fun fileKind(mime: String?): FileKind {
    val type = mime?.lowercase().orEmpty()
    return when {
        type == "application/pdf" -> FileKind("PDF", Icons.Default.PictureAsPdf, Color(0xFFE11D48), Color(0xFFFDECEC))
        type.startsWith("image/") -> FileKind("IMG", Icons.Default.Image, Color(0xFF3D5AFE), Color(0xFFE8EEFF))
        type.contains("word") || type.contains("document") -> FileKind("DOC", Icons.Default.Description, Color(0xFF3D5A99), Color(0xFFE8EEF9))
        else -> FileKind("FILE", Icons.Default.Description, Color(0xFF5C6578), Color(0xFFEEF1F6))
    }
}

private fun metaLine(document: VaultDocumentEntity): String {
    val size = formatFileSize(document.sizeBytes)
    val category = document.category.takeIf { it.isNotBlank() }
    val whenLabel = "Updated ${relativeTime(document.createdAt)}"
    return listOfNotNull(size, category, whenLabel).joinToString(" • ")
}

private fun relativeTime(createdAt: Long): String {
    val delta = (System.currentTimeMillis() - createdAt).coerceAtLeast(0)
    val days = TimeUnit.MILLISECONDS.toDays(delta)
    return when {
        days <= 0L -> "today"
        days == 1L -> "1 day ago"
        days < 30L -> "$days days ago"
        else -> "${days / 30} mo ago"
    }
}

fun loadDocumentPreview(
    context: Context,
    document: VaultDocumentEntity
): Bitmap? {
    val file = File(context.filesDir, document.relativePath)

    if (!file.isFile) return null

    return try {
        when {
            document.mimeType?.startsWith("image/") == true -> {
                BitmapFactory.decodeFile(file.absolutePath)
            }

            document.mimeType == "application/pdf" -> {
                renderFirstPdfPage(file)
            }

            else -> null
        }
    } catch (_: Exception) {
        null
    }
}

private fun renderFirstPdfPage(file: File): Bitmap? {
    val descriptor = ParcelFileDescriptor.open(
        file,
        ParcelFileDescriptor.MODE_READ_ONLY
    )

    descriptor.use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            if (renderer.pageCount == 0) return null

            renderer.openPage(0).use { page ->
                val targetWidth = 400
                val scale = targetWidth.toFloat() / page.width
                val targetHeight = (page.height * scale)
                    .toInt()
                    .coerceAtLeast(1)

                val bitmap = createBitmap(targetWidth, targetHeight)

                bitmap.eraseColor(android.graphics.Color.WHITE)

                page.render(
                    bitmap,
                    null,
                    null,
                    PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                )

                return bitmap
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L ->
            String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0)

        else ->
            String.format(
                Locale.getDefault(),
                "%.1f MB",
                bytes / (1024.0 * 1024.0)
            )
    }
}
