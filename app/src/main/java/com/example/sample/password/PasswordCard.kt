package com.example.sample.password

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sample.password.domain.Password

private val Heart = Color(0xFFE11D48)
private val Ink = Color(0xFF1B2559)
private val Muted = Color(0xFF8B93A7)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PasswordCard(
    password: Password,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val (icon, tint, wash) = iconFor(password)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(wash),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = password.title,
                        color = Ink,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    CategoryPill(password.category)
                }
                Text(
                    text = password.username,
                    color = Color(0xFF5C6578),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (password.websiteUrl.isNotBlank()) {
                        Icon(
                            Icons.Default.Language,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Muted,
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        text = metaLine(password),
                        color = Muted,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (password.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (password.isFavorite) "Remove favourite" else "Add favourite",
                    tint = if (password.isFavorite) Heart else Color(0xFFC5CAD6),
                )
            }
            IconButton(onClick = onCopyClick) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy password",
                    tint = Color(0xFF9AA3B5),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(category: String) {
    val (bg, fg) = when (category.lowercase()) {
        "work" -> Color(0xFFE8EEF9) to Color(0xFF3D5A99)
        "family", "personal" -> Color(0xFFDDF6EA) to Color(0xFF1F8A4C)
        "finance" -> Color(0xFFE7F8EF) to Color(0xFF1B8A4A)
        "music", "entertainment" -> Color(0xFFE7F8EF) to Color(0xFF1B8A4A)
        else -> Color(0xFFEEF1F6) to Color(0xFF5C6578)
    }
    Text(
        text = category,
        color = fg,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

private fun iconFor(password: Password): Triple<ImageVector, Color, Color> {
    val title = password.title.lowercase()
    val category = password.category.lowercase()
    return when {
        "netflix" in title || "movie" in title || category == "entertainment" ->
            Triple(Icons.Default.Movie, Color(0xFFE11D48), Color(0xFFFDE8EC))
        "spotify" in title || "music" in title ->
            Triple(Icons.Default.GraphicEq, Color(0xFF1DB954), Color(0xFFE5F8EC))
        "chase" in title || "bank" in title || category == "finance" ->
            Triple(Icons.Default.AccountBalance, Color(0xFF3D5AFE), Color(0xFFE8EEFF))
        "google" in title || "mail" in title || "@" in password.username && category == "work" ->
            Triple(Icons.Default.Email, Color(0xFF5C6BC0), Color(0xFFEEF0FA))
        category == "work" ->
            Triple(Icons.Default.Work, Color(0xFF3D5AFE), Color(0xFFE8EEFF))
        else ->
            Triple(Icons.Default.Language, Color(0xFF5C6BC0), Color(0xFFEEF0FA))
    }
}

private fun metaLine(password: Password): String {
    if (password.websiteUrl.isNotBlank()) return password.websiteUrl
    if (password.notes.isNotBlank()) return password.notes
    val days = (System.currentTimeMillis() - password.updatedAt) / 86_400_000L
    return when {
        days < 1 -> "Updated today"
        days == 1L -> "Updated yesterday"
        days < 14 -> "Updated $days days ago"
        days < 60 -> "Updated ${days / 7} weeks ago"
        else -> "Updated ${days / 30} months ago"
    }
}
