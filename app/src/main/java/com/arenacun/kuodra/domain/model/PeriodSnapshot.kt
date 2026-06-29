package com.arenacun.kuodra.domain.model

import java.time.LocalDate

/** Línea (categoría) de un periodo cerrado. */
data class PeriodLine(
    val categoryName: String,
    val count: Int,
    val amount: Money,
    val tone: AvatarTone,
)

/**
 * Periodo de presupuesto **cerrado y congelado** (sucesor data-shaped de `SettlementRecord` para
 * Personal). Se guarda al cerrar manualmente: el total y el desglose quedan fijos aunque luego se
 * editen movimientos viejos. Persistido en Room y sincronizado.
 */
data class PeriodSnapshot(
    val id: String,
    val title: String,
    val periodStart: LocalDate,
    val periodEnd: LocalDate,
    val totalSpent: Money,
    /** Monto del presupuesto vigente al cerrar, o null si no había presupuesto activo. */
    val budgetAmount: Money?,
    val lines: List<PeriodLine>,
    val createdAt: Long,
)
