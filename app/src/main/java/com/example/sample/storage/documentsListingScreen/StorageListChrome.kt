package com.example.sample.storage.documentsListingScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sample.storage.data.VaultDocumentEntity
import java.util.Locale

@Composable
internal fun StorageSearchField(query: String, onQueryChange: (String) -> Unit, onClear: () -> Unit) {
    Surface(shape = RoundedCornerShape(28.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = Muted)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                textStyle = TextStyle(color = Navy, fontSize = 15.sp),
                decorationBox = { inner ->
                    if (query.isEmpty()) Text("Search documents, IDs, medical...", color = Muted, fontSize = 15.sp)
                    inner()
                },
            )
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear search", tint = Muted)
                }
            }
        }
    }
}

@Composable
internal fun StorageChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = if (selected) Navy else Color.White) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = if (selected) Color.White else Navy,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun UsageCard(documents: List<VaultDocumentEntity>) {
    val used = documents.sumOf { it.sizeBytes }
    val fraction = (used.toFloat() / QuotaBytes).coerceIn(0f, 1f)
    Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8EEFF)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Storage, contentDescription = null, tint = Color(0xFF3D5AFE))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Vault Storage", color = Navy, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Zero-Knowledge Encrypted", color = Muted, fontSize = 12.sp)
                }
                Text("${formatGb(used)} / 15 GB", color = Navy, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE8ECF4)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .background(Color(0xFF3D5AFE)),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("${(fraction * 100).toInt()}% used", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun RecordsHeader(count: Int, sortAz: Boolean, onToggleSort: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Encrypted Records", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Navy)
        Spacer(Modifier.size(8.dp))
        Text(
            "$count",
            color = Color(0xFF3D5AFE),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE8EEFF))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Spacer(Modifier.weight(1f))
        Surface(onClick = onToggleSort, shape = RoundedCornerShape(50), color = if (sortAz) Navy else Color.White) {
            Text(
                "A–Z",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = if (sortAz) Color.White else Navy,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
            )
        }
    }
}

internal fun formatGb(bytes: Long): String {
    val gb = bytes / (1024.0 * 1024.0 * 1024.0)
    return if (gb >= 0.1) {
        String.format(Locale.US, "%.1f GB", gb)
    } else {
        String.format(Locale.US, "%.0f MB", bytes / (1024.0 * 1024.0))
    }
}
