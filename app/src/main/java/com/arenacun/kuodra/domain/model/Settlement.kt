package com.arenacun.kuodra.domain.model

import java.time.LocalDate

/**
 * Transferencia sugerida para saldar un grupo: [fromId] le paga [amount] a [toId] (ids de persona,
 * [PersonRef.ME] incluido).
 */
data class Transfer(val fromId: String, val toId: String, val amount: Money)

/**
 * Línea de un corte: saldo neto de una persona en el momento de liquidar. [net] > 0 = le deben (el
 * grupo le debe); [net] < 0 = debe al grupo. El [name] se **congela** al cerrar para que el histórico
 * no cambie aunque el contacto se renombre o se borre.
 */
data class SettlementBalanceLine(val personId: String, val name: String, val net: Money)

/**
 * Tipo de registro en la colección de liquidaciones:
 * - [Corte]: cierre de todo el periodo (congela saldos y estampa los movimientos vivos).
 * - [Payment]: pago individual de una persona (parcial o total). Ajusta el saldo de esa persona sin
 *   estampar movimientos; lleva una sola línea y una sola transferencia.
 */
enum class SettlementKind { Corte, Payment }

/**
 * Corte/liquidación de un espacio de Gastos: congela los saldos por persona y las transferencias
 * sugeridas de un periodo. Sucesor data-shaped de `SettlementRecord` (que era todo-strings) para
 * Gastos, análogo a [PeriodSnapshot] en Personal. Persistible y sincronizable.
 *
 * Un [kind] = [SettlementKind.Payment] representa un pago individual: [settledBy] guarda el id del
 * corte que lo "consumió" (`""` = vivo, aún cuenta en los balances).
 */
data class Settlement(
    val id: String,
    val spaceId: String,
    val title: String,
    val date: LocalDate,
    val total: Money,
    val lines: List<SettlementBalanceLine>,
    val transfers: List<Transfer>,
    val createdAt: Long,
    val kind: SettlementKind = SettlementKind.Corte,
    val settledBy: String = "",
)
