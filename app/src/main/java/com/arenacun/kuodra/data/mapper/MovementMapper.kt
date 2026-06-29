package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.local.db.MovementEntity
import com.arenacun.kuodra.data.remote.dto.MovementDto
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import java.time.LocalDate

/** Entity → dominio (la UI/dominio no conoce `owner` ni metadatos de sync). */
fun MovementEntity.toDomain(): Movement = Movement(
    id = id,
    amount = Money(amountCents),
    categoryId = categoryId,
    title = title,
    note = note,
    date = date,
    payer = payer,
    splitNames = splitNames,
)

/** Dominio → Entity, sellando `owner` y los metadatos de sincronización. */
fun Movement.toEntity(
    owner: String,
    updatedAt: Long,
    dirty: Boolean,
    deleted: Boolean = false,
): MovementEntity = MovementEntity(
    id = id,
    owner = owner,
    amountCents = amount.cents,
    categoryId = categoryId,
    title = title,
    note = note,
    date = date,
    payer = payer,
    splitNames = splitNames,
    updatedAt = updatedAt,
    deleted = deleted,
    dirty = dirty,
)

/** Entity → DTO (push al servidor). PocketBase ignora los campos de sistema (`updated`). */
fun MovementEntity.toDto(): MovementDto = MovementDto(
    id = id,
    owner = owner,
    amount = amountCents,
    category = categoryId,
    title = title,
    note = note,
    date = date.toString(),
    payer = payer,
    splitNames = splitNames,
    deleted = deleted,
    updated = remoteUpdated,
)

/** DTO → Entity (pull del servidor): ya sincronizado (`dirty = false`), con el `updated` remoto. */
fun MovementDto.toEntity(owner: String): MovementEntity = MovementEntity(
    id = id,
    owner = owner.ifEmpty { this.owner },
    amountCents = amount,
    categoryId = category,
    title = title,
    note = note,
    date = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
    payer = payer,
    splitNames = splitNames,
    updatedAt = System.currentTimeMillis(),
    deleted = deleted,
    dirty = false,
    remoteUpdated = updated,
)
