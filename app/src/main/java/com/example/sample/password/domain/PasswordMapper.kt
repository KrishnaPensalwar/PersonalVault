package com.example.sample.password.domain

import com.example.sample.password.data.PasswordEntity

// ── Entity → Domain ──────────────────────────────────────────────────────────

fun PasswordEntity.toDomain(): Password = Password(
    id = id,
    title = title,
    username = username,
    password = password,
    websiteUrl = websiteUrl,
    notes = notes,
    category = category,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun List<PasswordEntity>.toDomain(): List<Password> = map { it.toDomain() }

// ── Domain → Entity ──────────────────────────────────────────────────────────

fun Password.toEntity(): PasswordEntity = PasswordEntity(
    id = id,
    title = title,
    username = username,
    password = password,
    websiteUrl = websiteUrl,
    notes = notes,
    category = category,
    isFavorite = isFavorite,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
