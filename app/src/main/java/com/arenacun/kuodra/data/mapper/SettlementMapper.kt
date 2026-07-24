package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.local.db.SettlementEntity
import com.arenacun.kuodra.data.remote.dto.SettlementDto
import com.arenacun.kuodra.data.remote.dto.SettlementLineDto
import com.arenacun.kuodra.data.remote.dto.TransferDto
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.SettlementBalanceLine
import com.arenacun.kuodra.domain.model.SettlementLine
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.Transfer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val json = Json { ignoreUnknownKeys = true }

private fun List<SettlementBalanceLine>.linesToJson(): String =
    json.encodeToString(map { SettlementLineDto(it.personId, it.name, it.net.cents) })

private fun String.toLines(): List<SettlementBalanceLine> =
    if (isBlank()) emptyList()
    else json.decodeFromString<List<SettlementLineDto>>(this)
        .map { SettlementBalanceLine(it.personId, it.name, Money(it.net)) }

private fun List<Transfer>.transfersToJson(): String =
    json.encodeToString(map { TransferDto(it.fromId, it.toId, it.amount.cents) })

private fun String.toTransfers(): List<Transfer> =
    if (isBlank()) emptyList()
    else json.decodeFromString<List<TransferDto>>(this)
        .map { Transfer(it.fromId, it.toId, Money(it.amount)) }

/** Entity → dominio. */
fun SettlementEntity.toDomain(): Settlement = Settlement(
    id = id,
    spaceId = space,
    title = title,
    date = date,
    total = Money(totalCents),
    lines = linesJson.toLines(),
    transfers = transfersJson.toTransfers(),
    createdAt = createdAt,
)

/** Dominio → Entity, sellando owner y metadatos de sincronización. */
fun Settlement.toEntity(
    owner: String,
    updatedAt: Long,
    dirty: Boolean,
    deleted: Boolean = false,
): SettlementEntity = SettlementEntity(
    id = id,
    owner = owner,
    space = spaceId,
    title = title,
    date = date,
    totalCents = total.cents,
    linesJson = lines.linesToJson(),
    transfersJson = transfers.transfersToJson(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    deleted = deleted,
    dirty = dirty,
)

/** Entity → DTO (push). */
fun SettlementEntity.toDto(): SettlementDto = SettlementDto(
    id = id,
    owner = owner,
    space = space,
    title = title,
    date = date.toString(),
    total = totalCents,
    lines = json.decodeFromString(linesJson.ifBlank { "[]" }),
    transfers = json.decodeFromString(transfersJson.ifBlank { "[]" }),
    createdAt = createdAt,
    deleted = deleted,
    updated = remoteUpdated,
)

/** Proyección al modelo de la UI de historial (display) para Gastos. */
fun Settlement.toSettlementRecord(): SettlementRecord = SettlementRecord(
    id = id,
    title = title,
    periodLabel = DateLabels.dayMonthYear(date),
    total = Calc.formatAmount(total.major),
    statLabel = "${lines.size} ${if (lines.size == 1) "persona" else "personas"}",
    lines = lines
        .filter { it.personId != PersonRef.ME }
        .map { line ->
            val owes = line.net.cents < 0L
            SettlementLine(
                name = line.name,
                detail = if (owes) "te debía" else "le debías",
                amount = (if (owes) "+" else "−") + Calc.formatAmount(kotlin.math.abs(line.net.cents) / 100.0),
                tone = if (owes) AvatarTone.Pos else AvatarTone.Neg,
                positive = owes,
            )
        },
)

/** DTO → Entity (pull): ya sincronizado (`dirty = false`), con el `updated` remoto. */
fun SettlementDto.toEntity(owner: String): SettlementEntity = SettlementEntity(
    id = id,
    owner = owner.ifEmpty { this.owner },
    space = space,
    title = title,
    date = runCatching { LocalDate.parse(date) }.getOrDefault(LocalDate.now()),
    totalCents = total,
    linesJson = json.encodeToString(lines),
    transfersJson = json.encodeToString(transfers),
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis(),
    deleted = deleted,
    dirty = false,
    remoteUpdated = updated,
)
