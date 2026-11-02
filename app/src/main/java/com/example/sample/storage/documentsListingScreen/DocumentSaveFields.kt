package com.example.sample.storage.documentsListingScreen

import android.graphics.Bitmap
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun SheetBody(
    source: VaultSource,
    noteBody: String,
    onNoteChange: (String) -> Unit,
    preview: Bitmap?,
    onScan: () -> Unit,
    onPickFile: () -> Unit,
) {
    if (source == VaultSource.Note) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFF7F8FB), modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = noteBody,
                onValueChange = onNoteChange,
                textStyle = TextStyle(color = Navy, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth().height(140.dp).padding(14.dp),
                decorationBox = { inner ->
                    if (noteBody.isEmpty()) Text("Write a private note", color = Muted, fontSize = 15.sp)
                    inner()
                },
            )
        }
        return
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFD5DBE7), RoundedCornerShape(16.dp))
            .clickable(onClick = if (source == VaultSource.Scan) onScan else onPickFile),
        contentAlignment = Alignment.Center,
    ) {
        if (preview != null) {
            Image(preview.asImageBitmap(), "Document preview", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    if (source == VaultSource.Scan) Icons.Default.CameraAlt else Icons.Default.CloudUpload,
                    contentDescription = null,
                    tint = Muted,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (source == VaultSource.Scan) "Tap to open camera and scan" else "Tap to browse files or drop document here",
                    color = Navy,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    if (source == VaultSource.Scan) "Saves a photo of the document" else "Supports PDF, PNG, JPG, DOCX",
                    color = Muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
internal fun TitleField(editableName: String, onNameChange: (String) -> Unit) {
    Spacer(Modifier.height(16.dp))
    Text("Document Title *", color = BodyColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF7F8FB), modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp)) {
            BasicTextField(
                value = editableName,
                onValueChange = onNameChange,
                singleLine = true,
                textStyle = TextStyle(color = Navy, fontSize = 15.sp),
                modifier = Modifier.weight(1f).padding(vertical = 14.dp),
                decorationBox = { inner ->
                    if (editableName.isEmpty()) Text("Document name", color = Muted, fontSize = 15.sp)
                    inner()
                },
            )
            if (editableName.isNotBlank()) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF1F8A4C), modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun CategoryRow(
    category: String,
    customCategory: String,
    onCategoryChange: (String) -> Unit,
    onCustomCategoryChange: (String) -> Unit,
) {
    Spacer(Modifier.height(14.dp))
    Text("Category (optional)", color = BodyColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StorageChip("None", category.isEmpty()) { onCategoryChange("") }
        SaveCategories.forEach { item ->
            StorageChip(item, category == item) { onCategoryChange(item) }
        }
        StorageChip(OtherCategory, category == OtherCategory) { onCategoryChange(OtherCategory) }
    }
    if (category == OtherCategory) {
        Spacer(Modifier.height(8.dp))
        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFF7F8FB), modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = customCategory,
                onValueChange = onCustomCategoryChange,
                singleLine = true,
                textStyle = TextStyle(color = Navy, fontSize = 15.sp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
                decorationBox = { inner ->
                    if (customCategory.isEmpty()) Text("Custom category", color = Muted, fontSize = 15.sp)
                    inner()
                },
            )
        }
    }
}

@Composable
internal fun SaveButton(canSave: Boolean, onSave: () -> Unit) {
    Surface(
        onClick = onSave,
        enabled = canSave,
        shape = RoundedCornerShape(16.dp),
        color = if (canSave) Navy else Color(0xFFC5CAD6),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Encrypt & Save to Vault", color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}
