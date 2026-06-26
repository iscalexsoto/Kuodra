package com.arenacun.kuodra.domain.model

/** Línea de un corte/liquidación histórico (una persona y su saldo del periodo). */
data class SettlementLine(
    val name: String,
    val detail: String,
    val amount: String,
    val tone: AvatarTone,
    /** true = a favor, false = en contra, null = neutro. */
    val positive: Boolean?,
)

/**
 * Registro de un periodo cerrado (`scrHistory` / `scrHistoryDetail`): una liquidación de
 * gastos, un corte de caja o el cierre de un periodo de presupuesto personal.
 */
data class SettlementRecord(
    val id: String,
    val title: String,
    val periodLabel: String,
    val total: String,
    val statLabel: String,
    val lines: List<SettlementLine>,
)
