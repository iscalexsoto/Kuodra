package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.local.db.SpaceEntity
import com.arenacun.kuodra.data.remote.dto.SpaceDto
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase

/** Entity → dominio (los espacios de Room son siempre de Gastos). */
fun SpaceEntity.toDomain(): Space = Space(
    id = id,
    useCase = UseCase.Gastos,
    name = name,
    archived = archived,
    reminderEnabled = reminderEnabled,
)

/** Entity → DTO (push). PocketBase ignora los campos de sistema (`updated`). */
fun SpaceEntity.toDto(): SpaceDto = SpaceDto(
    id = id,
    owner = owner,
    name = name,
    archived = archived,
    reminderEnabled = reminderEnabled,
    deleted = deleted,
    updated = remoteUpdated,
)

/** DTO → Entity (pull): ya sincronizado (`dirty = false`), con el `updated` remoto. */
fun SpaceDto.toEntity(owner: String): SpaceEntity = SpaceEntity(
    id = id,
    owner = owner.ifEmpty { this.owner },
    name = name,
    archived = archived,
    reminderEnabled = reminderEnabled,
    updatedAt = System.currentTimeMillis(),
    deleted = deleted,
    dirty = false,
    remoteUpdated = updated,
)
