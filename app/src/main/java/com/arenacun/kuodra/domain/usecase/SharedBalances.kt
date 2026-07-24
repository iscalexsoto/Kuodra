package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Movement

/**
 * Balances por persona de un espacio de Gastos, en centavos. Puro y testeable.
 *
 * Por cada movimiento vivo: cada pagador suma lo que puso (`+amount`) y cada participante de la
 * división resta su parte (`−share`). El neto de una persona: **positivo** = puso más de lo que le
 * tocaba ⇒ el grupo le debe; **negativo** = debe al grupo. Como los `splits` ya vienen resueltos a
 * centavos exactos que suman el total (ver [SplitCalc]), la suma de todos los netos es cero.
 */
object SharedBalances {

    /** Ignora los movimientos ya liquidados (`settlementId != ""`). Devuelve solo netos distintos de cero. */
    fun compute(movements: List<Movement>): Map<String, Long> {
        val balance = mutableMapOf<String, Long>()
        for (movement in movements) {
            if (movement.settlementId.isNotEmpty()) continue
            for (payer in movement.payers) {
                balance[payer.personId] = (balance[payer.personId] ?: 0L) + payer.amount.cents
            }
            for (split in movement.splits) {
                balance[split.personId] = (balance[split.personId] ?: 0L) - split.share.cents
            }
        }
        return balance.filterValues { it != 0L }
    }
}
