package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.local.db.CategoryEntity
import com.arenacun.kuodra.data.remote.dto.CategoryDto
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Category

fun CategoryEntity.toDomain(): Category = Category(
    id = id,
    name = name,
    tag = tag,
    tone = runCatching { AvatarTone.valueOf(tone) }.getOrDefault(AvatarTone.Tint),
    archived = archived,
)

fun Category.toEntity(
    owner: String,
    updatedAt: Long,
    dirty: Boolean,
    deleted: Boolean = false,
): CategoryEntity = CategoryEntity(
    id = id,
    owner = owner,
    name = name,
    tag = tag,
    tone = tone.name,
    archived = archived,
    updatedAt = updatedAt,
    deleted = deleted,
    dirty = dirty,
)

fun CategoryEntity.toDto(): CategoryDto = CategoryDto(
    id = id,
    owner = owner,
    name = name,
    tag = tag,
    tone = tone,
    archived = archived,
    deleted = deleted,
    updated = remoteUpdated,
)

fun CategoryDto.toEntity(owner: String): CategoryEntity = CategoryEntity(
    id = id,
    owner = owner.ifEmpty { this.owner },
    name = name,
    tag = tag,
    tone = tone,
    archived = archived,
    updatedAt = System.currentTimeMillis(),
    deleted = deleted,
    dirty = false,
    remoteUpdated = updated,
)
