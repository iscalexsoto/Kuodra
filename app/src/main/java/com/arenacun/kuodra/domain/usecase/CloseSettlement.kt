package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.SettlementBalanceLine
import com.arenacun.kuodra.domain.model.SettlementKind
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.newId
import com.arenacun.kuodra.domain.model.total
import java.time.LocalDate

/**
 * Cierre de un periodo de Gastos: congela los saldos vivos de un espacio en un [Settlement] y
 * devuelve los ids de los movimientos **y de los pagos vivos** que hay que estampar/consumir con ese
 * corte (para que dejen de contar en los balances). Análogo a [ClosePeriod] en Personal. Puro.
 */
object CloseSettlement {

    data class Result(
        val settlement: Settlement,
        val movementIds: List<String>,
        val paymentIds: List<String>,
    )

    fun build(
        spaceId: String,
        spaceName: String,
        movements: List<Movement>,
        persons: Map<String, SpacePerson>,
        today: LocalDate,
        settlements: List<Settlement> = emptyList(),
    ): Result {
        val live = movements.filter { it.spaceId == spaceId && it.settlementId.isEmpty() }
        val livePayments = settlements.filter {
            it.spaceId == spaceId && it.kind == SettlementKind.Payment && it.settledBy.isEmpty()
        }
        val balances = SharedBalances.compute(live, livePayments)
        val transfers = SettleSuggestions.compute(balances)
        val lines = balances
            .map { (id, net) -> SettlementBalanceLine(id, nameOf(id, persons), Money(net)) }
            .sortedByDescending { it.net.cents }
        val total = live.map { it.amount }.total()
        val settlement = Settlement(
            id = newId(),
            spaceId = spaceId,
            title = "Liquidación de ${DateLabels.monthName(today)}",
            date = today,
            total = total,
            lines = lines,
            transfers = transfers,
            createdAt = System.currentTimeMillis(),
        )
        return Result(settlement, live.map { it.id }, livePayments.map { it.id })
    }

    private fun nameOf(personId: String, persons: Map<String, SpacePerson>): String = when {
        personId == PersonRef.ME -> "Tú"
        else -> persons[personId]?.name ?: "(eliminado)"
    }
}
