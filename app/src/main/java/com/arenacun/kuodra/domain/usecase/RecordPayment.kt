package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.SettlementBalanceLine
import com.arenacun.kuodra.domain.model.SettlementKind
import com.arenacun.kuodra.domain.model.Transfer
import com.arenacun.kuodra.domain.model.newId
import java.time.LocalDate

/**
 * Construye un pago individual (parcial o total) de una persona como [Settlement] de
 * [SettlementKind.Payment]. Puro y testeable. La dirección la decide el signo de [currentNet] (el
 * saldo vivo de la persona): **< 0** (te debe) ⇒ te paga (`persona → Tú`); **> 0** (le debes) ⇒
 * le pagas (`Tú → persona`). El [amount] se acota a lo pendiente para no sobre-liquidar.
 */
object RecordPayment {

    fun build(
        spaceId: String,
        personId: String,
        personName: String,
        amount: Money,
        currentNet: Long,
        today: LocalDate,
    ): Settlement {
        val capped = Money(amount.cents.coerceIn(0, kotlin.math.abs(currentNet)))
        // currentNet < 0 (te debe): la persona te paga. currentNet >= 0 (le debes): tú le pagas.
        val transfer = if (currentNet < 0L)
            Transfer(fromId = personId, toId = PersonRef.ME, amount = capped)
        else
            Transfer(fromId = PersonRef.ME, toId = personId, amount = capped)
        return Settlement(
            id = newId(),
            spaceId = spaceId,
            title = "Pago de $personName",
            date = today,
            total = capped,
            lines = listOf(SettlementBalanceLine(personId, personName, Money(currentNet))),
            transfers = listOf(transfer),
            createdAt = System.currentTimeMillis(),
            kind = SettlementKind.Payment,
            settledBy = "",
        )
    }
}
