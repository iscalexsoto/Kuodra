package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.ReturnStatus
import com.arenacun.kuodra.domain.model.total

/**
 * Cálculo de devoluciones (solo Personal). Puro y testeable. El % es global y vive en
 * `BudgetConfig.returnPercent`; los movimientos [ReturnStatus.Pending] lo aplican en vivo, y al
 * marcarse [ReturnStatus.Returned] el % vigente se congela en `Movement.returnPercent` para que el
 * histórico no cambie al reajustar el global (regla forward-only).
 */
object ReturnCalc {

    const val DEFAULT_RETURN_PERCENT = 75

    /**
     * Base devolvible en centavos. Sin partidas ⇒ el total completo. Con partidas ⇒ reparte el
     * total en proporción a las partidas marcadas [MovementItem.returnable]; así el "Ajuste" (total
     * − suma de partidas, que en Kuodra no se persiste como partida) se distribuye implícitamente.
     */
    fun returnableBase(total: Money, items: List<MovementItem>): Money {
        if (items.isEmpty()) return total
        val allCents = items.map { it.amount }.total().cents
        if (allCents == 0L) return Money.Zero
        val selCents = items.filter { it.returnable }.map { it.amount }.total().cents
        return Money(Math.round(total.cents * (selCents.toDouble() / allCents)))
    }

    /** Reembolso EN VIVO de un movimiento al % global vigente; [Money.Zero] si no está Pending. */
    fun returnAmount(movement: Movement, currentPercent: Int): Money =
        if (movement.returnStatus == ReturnStatus.Pending)
            percentOf(returnableBase(movement.amount, movement.items), currentPercent)
        else Money.Zero

    /** Reembolso CONGELADO de un movimiento devuelto al % estampado; [Money.Zero] si no está Returned. */
    fun returnedAmount(movement: Movement): Money =
        if (movement.returnStatus == ReturnStatus.Returned)
            percentOf(returnableBase(movement.amount, movement.items), movement.returnPercent ?: DEFAULT_RETURN_PERCENT)
        else Money.Zero

    /** Total "por cobrar": suma del reembolso vivo de TODOS los movimientos Pending. */
    fun pendingTotal(movements: List<Movement>, currentPercent: Int): Money =
        movements.map { returnAmount(it, currentPercent) }.total()

    private fun percentOf(base: Money, percent: Int): Money =
        Money(Math.round(base.cents * percent / 100.0))
}
