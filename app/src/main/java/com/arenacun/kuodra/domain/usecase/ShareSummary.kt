package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.SpacePerson

/**
 * Arma el **texto** del resumen de un grupo de Gastos para compartir por el share nativo de Android
 * (WhatsApp, notas, etc.). Puro (el intent vive en presentation). Reúne los saldos por persona
 * ([SharedBalances]) y las transferencias sugeridas para saldar ([SettleSuggestions]).
 *
 * El saldo de una persona es su neto respecto al grupo: **> 0** = el grupo le debe (le deben);
 * **< 0** = le debe al grupo (debe). El dueño es [PersonRef.ME] ("Tú").
 */
object ShareSummary {

    fun build(
        spaceName: String,
        movements: List<Movement>,
        contacts: Map<String, SpacePerson>,
        settlements: List<Settlement> = emptyList(),
    ): String {
        val nameOf: (String) -> String = { id ->
            if (id == PersonRef.ME) "Tú" else contacts[id]?.name ?: "(eliminado)"
        }
        val balances = SharedBalances.compute(movements, settlements)
        val nonZero = balances.filterValues { it != 0L }

        val sb = StringBuilder()
        sb.append("Resumen de \"$spaceName\"")
        if (nonZero.isEmpty()) {
            sb.append("\n\nTodo está saldado. No hay deudas pendientes.")
            return sb.toString()
        }

        sb.append("\n\nSaldos:")
        nonZero.entries
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
            .forEach { (id, net) ->
                val amount = Calc.formatAmount(kotlin.math.abs(net) / 100.0)
                val line = if (net > 0L) "le deben $amount a ${nameOf(id)}" else "${nameOf(id)} debe $amount"
                sb.append("\n• $line")
            }

        val transfers = SettleSuggestions.compute(balances)
        if (transfers.isNotEmpty()) {
            sb.append("\n\nPara saldar:")
            transfers.forEach { t ->
                sb.append("\n• ${nameOf(t.fromId)} → ${nameOf(t.toId)}: ${Calc.formatAmount(t.amount.cents / 100.0)}")
            }
        }
        return sb.toString()
    }
}
