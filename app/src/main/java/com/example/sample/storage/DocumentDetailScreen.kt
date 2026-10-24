package com.example.sample.storage

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sample.navigation.LocalBackStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.sample.storage.data.VaultDocumentEntity

@Composable
fun DocumentDetailScreen(
    document: SelectedDocument? = null
) {
    Log.d("TAG", "DocumentDetailScreen: $document")
    if (document == null) return
    
    val context = LocalContext.current
    
    val preview by produceState<Bitmap?>(
        initialValue = null,
        key1 = document.uri,
        key2 = document.mimeType
    ) {
        value = withContext(Dispatchers.IO) {
            val vaultEntity = VaultDocumentEntity(
                id = "", 
                name = document.fileName, 
                relativePath = document.uri, 
                mimeType = document.mimeType, 
                sizeBytes = document.size ?: 0L
            )
            loadDocumentPreview(context, vaultEntity)
        }
    }

    val backStack = LocalBackStack.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.clickable{
                backStack.removeLastOrNull()
            })
            Text(document.fileName)
        }

        Spacer(Modifier.height(24.dp))

       Column(modifier = Modifier.padding(24.dp)) {
            if (preview != null) {
                Image(
                    bitmap = preview!!.asImageBitmap(),
                    contentDescription = "Document Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.InsertDriveFile,
                    contentDescription = "Document Preview",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

           Spacer(Modifier.height(24.dp))

           DocumentDetailRow("Name", document.fileName)
           DocumentDetailRow(
               "Type",
               document.mimeType?.substringAfter("/")?.uppercase() ?: "UNKNOWN"
           )
           DocumentDetailRow("Size", "%.2f KB".format((document.size ?: 0) / 1024.0))
       }
    }
}

@Composable
fun DocumentDetailRow(
    title: String,
    value: String
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Text(title)
        Text(value)
    }
}