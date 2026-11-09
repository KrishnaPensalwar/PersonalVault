package com.example.sample.storage.documentsListingScreen

import androidx.compose.ui.graphics.Color

internal val Navy = Color(0xFF1B2559)
internal val Muted = Color(0xFF8B93A7)
internal val Heart = Color(0xFFE11D48)
internal val BodyColor = Color(0xFF5C6578)
internal val SaveCategories = listOf("Insurance", "Identity & IDs", "Financial", "Medical", "Property")
internal const val OtherCategory = "Other"
internal const val QuotaBytes = 15L * 1024 * 1024 * 1024

enum class VaultSource { Upload, Scan, Note }
