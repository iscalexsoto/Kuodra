package com.arenacun.kuodra.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Periodo cerrado (historial). Las líneas se guardan como JSON (`linesJson`). Montos en centavos.
 */
@Entity(tableName = "period_snapshots")
data class PeriodSnapshotEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val title: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalSpentCents: Long,
    val budgetAmountCents: Long?,
    val linesJson: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    val remoteUpdated: String = "",
)
