package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.SettlementKind

/**
 * Balances por persona de un espacio de Gastos, en centavos. Puro y testeable.
 *
 * Por cada movimiento vivo: cada pagador suma lo que puso (`+amount`) y cada participante de la
 * división resta su parte (`−share`). El neto de una persona: **positivo** = puso más de lo que le
 * tocaba ⇒ el grupo le debe; **negativo** = debe al grupo. Como los `splits` ya vienen resueltos a
 * centavos exactos que suman el total (ver [SplitCalc]), la suma de todos los netos es cero.
 *
 * Los **pagos individuales** vivos (kind=Payment, `settledBy == ""`) ajustan el saldo: cada pago es
 * una transferencia `from → to` que acerca a ambos a cero (`from += monto`, `to -= monto`).
 */
object SharedBalances {

    /**
     * Ignora los movimientos ya liquidados (`settlementId != ""`) y aplica los pagos vivos de
     * [settlements]. Devuelve solo netos distintos de cero.
     */
    fun compute(movements: List<Movement>, settlements: List<Settlement> = emptyList()): Map<String, Long> {
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
        for (settlement in settlements) {
            if (settlement.kind != SettlementKind.Payment || settlement.settledBy.isNotEmpty()) continue
            for (t in settlement.transfers) {
                balance[t.fromId] = (balance[t.fromId] ?: 0L) + t.amount.cents
                balance[t.toId] = (balance[t.toId] ?: 0L) - t.amount.cents
            }
        }
        return balance.filterValues { it != 0L }
    }
}
