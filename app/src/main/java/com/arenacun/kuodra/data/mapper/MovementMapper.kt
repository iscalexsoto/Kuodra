package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.local.db.MovementEntity
import com.arenacun.kuodra.data.remote.dto.MovementDto
import com.arenacun.kuodra.data.remote.dto.MovementItemDto
import com.arenacun.kuodra.data.remote.dto.PayerShareDto
import com.arenacun.kuodra.data.remote.dto.SplitShareDto
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitShare
import com.arenacun.kuodra.domain.scan.ScanSource
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val json = Json { ignoreUnknownKeys = true }

/** Partidas (dominio) → JSON de almacenamiento, vía DTO `@Serializable`. */
private fun List<MovementItem>.toJson(): String =
    json.encodeToString(map { MovementItemDto(it.id, it.concept, it.amount.cents, it.payer) })

/** JSON de almacenamiento → partidas (dominio). */
private fun String.toItems(): List<MovementItem> =
    if (isBlank()) emptyList()
    else json.decodeFromString<List<MovementItemDto>>(this)
        .map { MovementItem(it.id, it.concept, Money(it.amount), it.payer) }

/** Pagadores/divisiones (dominio) ⇄ JSON de almacenamiento. */
private fun List<PayerShare>.payersToJson(): String =
    json.encodeToString(map { PayerShareDto(it.personId, it.amount.cents) })

private fun String.toPayers(): List<PayerShare> =
    if (isBlank()) emptyList()
    else json.decodeFromString<List<PayerShareDto>>(this).map { PayerShare(it.personId, Money(it.amount)) }

private fun List<SplitShare>.splitsToJson(): String =
    json.encodeToString(map { SplitShareDto(it.personId, it.share.cents) })

private fun String.toSplits(): List<SplitShare> =
    if (isBlank()) emptyList()
    else json.decodeFromString<List<SplitShareDto>>(this).map { SplitShare(it.personId, Money(it.share)) }

/** Nombre del enum ⇄ `SplitMode`, tolerante (vacío/desconocido ⇒ None). */
private fun splitModeOf(name: String): SplitMode =
    SplitMode.entries.firstOrNull { it.name == name } ?: SplitMode.None

/** Entity → dominio (la UI/dominio no conoce `owner` ni metadatos de sync). */
fun MovementEntity.toDomain(): Movement = Movement(
    id = id,
    amount = Money(amountCents),
    categoryId = categoryId,
    title = title,
    note = note,
    date = date,
    spaceId = space,
    payers = payersJson.toPayers(),
    splitMode = splitModeOf(splitMode),
    splits = splitsJson.toSplits(),
    settlementId = settlementId,
    items = itemsJson.toItems(),
    scanRawText = scanRawText,
    scanSource = scanSource?.let { name -> ScanSource.entries.firstOrNull { it.name == name } },
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
    space = spaceId,
    payersJson = payers.payersToJson(),
    splitMode = splitMode.name,
    splitsJson = splits.splitsToJson(),
    settlementId = settlementId,
    itemsJson = items.toJson(),
    scanRawText = scanRawText,
    scanSource = scanSource?.name,
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
    space = space,
    payers = json.decodeFromString(payersJson.ifBlank { "[]" }),
    splitMode = splitMode,
    splits = json.decodeFromString(splitsJson.ifBlank { "[]" }),
    settlementId = settlementId,
    items = json.decodeFromString(itemsJson.ifBlank { "[]" }),
    scanRawText = scanRawText.orEmpty(),
    scanSource = scanSource.orEmpty(),
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
    space = space,
    payersJson = json.encodeToString(payers),
    splitMode = splitMode.ifEmpty { "None" },
    splitsJson = json.encodeToString(splits),
    settlementId = settlementId,
    itemsJson = json.encodeToString(items),
    scanRawText = scanRawText.ifEmpty { null },
    scanSource = scanSource.ifEmpty { null },
    updatedAt = System.currentTimeMillis(),
    deleted = deleted,
    dirty = false,
    remoteUpdated = updated,
)
