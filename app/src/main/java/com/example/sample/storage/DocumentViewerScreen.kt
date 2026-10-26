package com.example.sample.storage

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sample.common.FullImageViewer
import com.example.sample.common.FullPdfViewer
import com.example.sample.common.FullTextViewer
import com.example.sample.common.UnsupportedDocViewer
import com.example.sample.navigation.Destinations
import com.example.sample.navigation.LocalBackStack
import com.example.sample.storage.data.VaultDocumentEntity
import com.example.sample.storage.documentDetailScreen.DocumentDetailUiState
import com.example.sample.storage.documentDetailScreen.DocumentDetailViewModel
import org.koin.androidx.compose.koinViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewerScreen(
    id: String,
    viewModel: DocumentDetailViewModel = koinViewModel()
) {
    val backStack = LocalBackStack.current
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(id) {
        viewModel.loadDocument(id)
    }

    when (val state = uiState) {
        is DocumentDetailUiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is DocumentDetailUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { viewModel.loadDocument(id) }) {
                        Text("Retry")
                    }
                    TextButton(onClick = { backStack.removeLastOrNull() }) {
                        Text("Go Back")
                    }
                }
            }
        }

        is DocumentDetailUiState.Success -> {
            val doc = state.document
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = doc.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        windowInsets = WindowInsets(0, 0, 0, 0),
                        navigationIcon = {
                            IconButton(onClick = { backStack.removeLastOrNull() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { openDocumentInExternalApp(context, doc) }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = "Open in external app"
                                )
                            }
                            IconButton(onClick = {
                                backStack.add(Destinations.DocumentDetailScreen(id = doc.id))
                            }) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit document"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    DocumentViewerContent(
                        doc = doc,
                        onOpenExternally = { openDocumentInExternalApp(context, doc) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentViewerContent(
    doc: VaultDocumentEntity,
    onOpenExternally: () -> Unit
) {
    val context = LocalContext.current
    val file = remember(doc.relativePath) { File(context.filesDir, doc.relativePath) }

    if (!file.exists()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Document file not found on device.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    val mime = doc.mimeType?.lowercase().orEmpty()

    when {
        mime.startsWith("image/") || isImageFile(doc.name) -> {
            FullImageViewer(file = file, docName = doc.name)
        }

        mime == "application/pdf" || doc.name.endsWith(".pdf", ignoreCase = true) -> {
            FullPdfViewer(file = file)
        }

        mime.startsWith("text/") || doc.name.endsWith(".txt", ignoreCase = true) -> {
            FullTextViewer(file = file)
        }

        else -> {
            UnsupportedDocViewer(
                doc = doc,
                onOpenExternally = onOpenExternally
            )
        }
    }
}


private fun isImageFile(name: String): Boolean {
    val ext = name.substringAfterLast(".", "").lowercase()
    return ext in listOf("png", "jpg", "jpeg", "webp", "gif", "bmp")
}

fun openDocumentInExternalApp(context: Context, document: VaultDocumentEntity) {
    try {
        val file = File(context.filesDir, document.relativePath)
        if (!file.exists()) {
            Toast.makeText(context, "File not found on device", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, document.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        Toast.makeText(context, "Cannot open document: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
