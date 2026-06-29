package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.local.db.PeriodSnapshotEntity
import com.arenacun.kuodra.data.remote.dto.PeriodLineDto
import com.arenacun.kuodra.data.remote.dto.PeriodSnapshotDto
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.PeriodLine
import com.arenacun.kuodra.domain.model.PeriodSnapshot
import com.arenacun.kuodra.domain.model.SettlementLine
import com.arenacun.kuodra.domain.model.SettlementRecord
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.math.roundToInt

private val json = Json { ignoreUnknownKeys = true }

private fun toneOf(name: String): AvatarTone =
    runCatching { AvatarTone.valueOf(name) }.getOrDefault(AvatarTone.Tint)

private fun parseDate(value: String): LocalDate =
    runCatching { LocalDate.parse(value) }.getOrDefault(LocalDate.now())

fun PeriodSnapshotEntity.toDomain(): PeriodSnapshot {
    val lines = json.decodeFromString<List<PeriodLineDto>>(linesJson).map {
        PeriodLine(it.categoryName, it.count, Money(it.amount), toneOf(it.tone))
    }
    return PeriodSnapshot(
        id = id,
        title = title,
        periodStart = periodStart,
        periodEnd = periodEnd,
        totalSpent = Money(totalSpentCents),
        budgetAmount = budgetAmountCents?.let { Money(it) },
        lines = lines,
        createdAt = createdAt,
    )
}

fun PeriodSnapshot.toEntity(
    owner: String,
    updatedAt: Long,
    dirty: Boolean,
    deleted: Boolean = false,
    remoteUpdated: String = "",
): PeriodSnapshotEntity {
    val linesJson = json.encodeToString(
        lines.map { PeriodLineDto(it.categoryName, it.count, it.amount.cents, it.tone.name) },
    )
    return PeriodSnapshotEntity(
        id = id,
        owner = owner,
        title = title,
        periodStart = periodStart,
        periodEnd = periodEnd,
        totalSpentCents = totalSpent.cents,
        budgetAmountCents = budgetAmount?.cents,
        linesJson = linesJson,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        dirty = dirty,
        remoteUpdated = remoteUpdated,
    )
}

fun PeriodSnapshotEntity.toDto(): PeriodSnapshotDto = PeriodSnapshotDto(
    id = id,
    owner = owner,
    title = title,
    periodStart = periodStart.toString(),
    periodEnd = periodEnd.toString(),
    totalSpent = totalSpentCents,
    budgetAmount = budgetAmountCents,
    lines = json.decodeFromString(linesJson),
    createdAt = createdAt,
    deleted = deleted,
    updated = remoteUpdated,
)

fun PeriodSnapshotDto.toEntity(owner: String): PeriodSnapshotEntity = PeriodSnapshotEntity(
    id = id,
    owner = owner.ifEmpty { this.owner },
    title = title,
    periodStart = parseDate(periodStart),
    periodEnd = parseDate(periodEnd),
    totalSpentCents = totalSpent,
    budgetAmountCents = budgetAmount,
    linesJson = json.encodeToString(lines),
    createdAt = createdAt,
    updatedAt = System.currentTimeMillis(),
    deleted = deleted,
    dirty = false,
    remoteUpdated = updated,
)

/** Proyección al modelo de la UI de historial (display). */
fun PeriodSnapshot.toSettlementRecord(): SettlementRecord {
    val periodLabel = "${DateLabels.dayMonth(periodStart)} – ${DateLabels.dayMonthYear(periodEnd)}"
    val stat = budgetAmount?.takeIf { it.cents > 0 }?.let { b ->
        val pct = (totalSpent.cents.toDouble() / b.cents * 100).roundToInt()
        if (totalSpent.cents <= b.cents) "${100 - pct}% bajo presupuesto" else "${pct - 100}% sobre presupuesto"
    } ?: "Sin presupuesto"
    return SettlementRecord(
        id = id,
        title = title,
        periodLabel = periodLabel,
        total = Calc.formatAmount(totalSpent.major),
        statLabel = stat,
        lines = lines.map {
            SettlementLine(
                name = it.categoryName,
                detail = "${it.count} ${if (it.count == 1) "movimiento" else "movimientos"}",
                amount = Calc.formatAmount(it.amount.major),
                tone = it.tone,
                positive = null,
            )
        },
    )
}
