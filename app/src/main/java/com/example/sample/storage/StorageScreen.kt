package com.example.sample.storage

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import org.koin.androidx.compose.koinViewModel
import com.example.sample.storage.data.VaultDocumentEntity
import java.util.UUID
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun StorageScreen(
    viewModel: StorageViewModel = koinViewModel()
) {
    val context = LocalContext.current

    var selectedDocument by remember {
        mutableStateOf<SelectedDocument?>(null)
    }

    var relativePath by remember {
        mutableStateOf<String?>(null)
    }

    val documents by viewModel.documents.collectAsState()

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val docInfo = getDocumentInfo(context, it)
            selectedDocument = docInfo
            val path = copyDocumentToInternalStorage(context, it)
            relativePath = path

            viewModel.saveDocument(
                VaultDocumentEntity(
                    id = UUID.randomUUID().toString(),
                    name = docInfo.fileName,
                    relativePath = path,
                    mimeType = docInfo.mimeType,
                    sizeBytes = docInfo.size ?: 0L
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Storage",
                fontSize = 24.sp
            )

            Button(
                onClick = {
                    documentPicker.launch(
                        arrayOf("*/*")
                    )
                }
            ) {
                Text("Add")
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
            items(documents) { doc ->
                StorageDocumentCard(
                    document = doc,
                    onClick = {
                        // We'll add full document viewing later.
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
