package com.example.sample.storage.documentsListingScreen

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sample.storage.SelectedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentSaveBottomSheet(
    sheetState: SheetState,
    selectedDocument: SelectedDocument?,
    selectedUri: Uri?,
    editableName: String,
    category: String = "",
    customCategory: String = "",
    onCustomCategoryChange: (String) -> Unit = {},
    onCategoryChange: (String) -> Unit = {},
    source: VaultSource = VaultSource.Upload,
    noteBody: String = "",
    onNoteChange: (String) -> Unit = {},
    onNameChange: (String) -> Unit,
    onPickFile: () -> Unit = {},
    onScan: () -> Unit = {},
    onSelectNote: () -> Unit = {},
    onDismissRequest: () -> Unit,
    onSave: () -> Unit,
) {
    val context = LocalContext.current
    val preview by produceState<Bitmap?>(null, selectedUri, selectedDocument?.mimeType) {
        value = withContext(Dispatchers.IO) {
            if (selectedUri != null && selectedDocument != null) {
                loadDocumentPreviewFromUri(context, selectedUri.toString(), selectedDocument.mimeType)
            } else {
                null
            }
        }
    }
    val canSave = when (source) {
        VaultSource.Note -> noteBody.isNotBlank()
        else -> selectedUri != null
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            SheetHeader(onDismissRequest)
            SourceRow(source, onScan, onPickFile, onSelectNote)
            Spacer(Modifier.height(14.dp))
            SheetBody(source, noteBody, onNoteChange, preview, onScan, onPickFile)
            TitleField(editableName, onNameChange)
            CategoryRow(category, customCategory, onCategoryChange, onCustomCategoryChange)
            Spacer(Modifier.height(20.dp))
            SaveButton(canSave, onSave)
            TextButton(onClick = onDismissRequest, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Cancel", color = Muted)
            }
        }
    }
}

@Composable
private fun SheetHeader(onDismissRequest: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Add to Vault Storage", color = Navy, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Zero-Knowledge AES-256 GCM",
                color = Color(0xFF1F8A4C),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFDDF6EA))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        IconButton(onClick = onDismissRequest) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Muted)
        }
    }
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SourceRow(
    source: VaultSource,
    onScan: () -> Unit,
    onPickFile: () -> Unit,
    onSelectNote: () -> Unit,
) {
    Text("SELECT SOURCE", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        SourceTile("Scan Doc", "Camera", Icons.Default.CameraAlt, source == VaultSource.Scan, Modifier.weight(1f), onScan)
        SourceTile("Upload File", "PDF, JPG, PNG", Icons.Default.CloudUpload, source == VaultSource.Upload, Modifier.weight(1f), onPickFile)
        SourceTile("Secure Note", "Plain Text", Icons.Default.Description, source == VaultSource.Note, Modifier.weight(1f), onSelectNote)
    }
}

