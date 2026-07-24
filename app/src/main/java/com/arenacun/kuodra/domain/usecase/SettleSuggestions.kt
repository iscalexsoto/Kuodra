package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Transfer

/**
 * A partir de los balances por persona ([SharedBalances]) sugiere el conjunto de transferencias que
 * salda el grupo. Greedy determinístico: empareja al mayor deudor con el mayor acreedor y transfiere
 * el mínimo de ambos, repitiendo hasta saldar. No garantiza el mínimo absoluto de transferencias
 * (problema NP-duro), pero da un resultado pequeño, estable y fácil de auditar. Puro y testeable.
 */
object SettleSuggestions {

    fun compute(balances: Map<String, Long>): List<Transfer> {
        // Acreedores (les deben, neto > 0) y deudores (deben, neto < 0 → magnitud positiva).
        val creditors = balances.filter { it.value > 0L }
            .map { it.key to it.value }
            .sortedWith(compareByDescending<Pair<String, Long>> { it.second }.thenBy { it.first })
            .toMutableList()
        val debtors = balances.filter { it.value < 0L }
            .map { it.key to -it.value }
            .sortedWith(compareByDescending<Pair<String, Long>> { it.second }.thenBy { it.first })
            .toMutableList()

        val transfers = mutableListOf<Transfer>()
        var ci = 0
        var di = 0
        var creditLeft = if (creditors.isNotEmpty()) creditors[0].second else 0L
        var debtLeft = if (debtors.isNotEmpty()) debtors[0].second else 0L

        while (ci < creditors.size && di < debtors.size) {
            val pay = minOf(creditLeft, debtLeft)
            if (pay > 0L) {
                transfers += Transfer(debtors[di].first, creditors[ci].first, Money(pay))
            }
            creditLeft -= pay
            debtLeft -= pay
            if (creditLeft == 0L) {
                ci++
                if (ci < creditors.size) creditLeft = creditors[ci].second
            }
            if (debtLeft == 0L) {
                di++
                if (di < debtors.size) debtLeft = debtors[di].second
            }
        }
        return transfers
    }
}
