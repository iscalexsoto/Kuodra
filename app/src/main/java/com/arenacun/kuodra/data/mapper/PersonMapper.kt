package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.local.db.PersonEntity
import com.arenacun.kuodra.data.remote.dto.PersonDto
import com.arenacun.kuodra.domain.model.SpacePerson

/** Entity → dominio (la UI no ve owner ni el espacio, que resuelve el repo). */
fun PersonEntity.toDomain(): SpacePerson = SpacePerson(
    id = id,
    name = name,
    phone = phone,
)

/** Dominio → Entity, sellando owner, espacio y metadatos de sincronización. */
fun SpacePerson.toEntity(
    owner: String,
    space: String,
    updatedAt: Long,
    dirty: Boolean,
    deleted: Boolean = false,
): PersonEntity = PersonEntity(
    id = id,
    owner = owner,
    space = space,
    name = name,
    phone = phone,
    updatedAt = updatedAt,
    deleted = deleted,
    dirty = dirty,
)

/** Entity → DTO (push). */
fun PersonEntity.toDto(): PersonDto = PersonDto(
    id = id,
    owner = owner,
    space = space,
    name = name,
    phone = phone,
    deleted = deleted,
    updated = remoteUpdated,
)

/** DTO → Entity (pull): ya sincronizado (`dirty = false`), con el `updated` remoto. */
fun PersonDto.toEntity(owner: String): PersonEntity = PersonEntity(
    id = id,
    owner = owner.ifEmpty { this.owner },
    space = space,
    name = name,
    phone = phone,
    updatedAt = System.currentTimeMillis(),
    deleted = deleted,
    dirty = false,
    remoteUpdated = updated,
)
