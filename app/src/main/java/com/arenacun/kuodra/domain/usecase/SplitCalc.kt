package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.SplitShare
import com.arenacun.kuodra.domain.model.total

/**
 * Resolución y validación de la división de un gasto compartido. Puro y testeable. Todas las
 * resoluciones devuelven partes en **centavos exactos que suman el total**; el remanente de redondeo
 * se reparte de forma determinística (un centavo a cada id, en orden) para que dos dispositivos
 * lleguen al mismo resultado.
 */
object SplitCalc {

    /** Partes iguales entre [ids]. El remanente (total mod n) se asigna a los primeros ids. */
    fun resolveEqual(total: Money, ids: List<String>): List<SplitShare> {
        if (ids.isEmpty()) return emptyList()
        val base = total.cents / ids.size
        val remainder = (total.cents - base * ids.size).toInt()
        return ids.mapIndexed { i, id ->
            SplitShare(id, Money(base + if (i < remainder) 1 else 0))
        }
    }

    /**
     * Reparte [total] según [percents] (porcentajes enteros que deben sumar 100). Usa el piso de
     * cada porción y reparte el remanente de a un centavo por orden de la lista.
     */
    fun resolvePercents(total: Money, percents: List<Pair<String, Int>>): List<SplitShare> {
        if (percents.isEmpty()) return emptyList()
        val base = percents.map { (id, pct) -> id to (total.cents * pct / 100) }
        var remainder = (total.cents - base.sumOf { it.second }).toInt()
        return base.map { (id, cents) ->
            val extra = if (remainder > 0) 1 else 0
            remainder -= extra
            SplitShare(id, Money(cents + extra))
        }
    }

    /** Mensaje de error si los pagadores no suman el total (o no hay ninguno); null si cuadra. */
    fun validatePayers(total: Money, payers: List<PayerShare>): String? {
        if (payers.isEmpty()) return "Falta indicar quién pagó"
        val sum = payers.map { it.amount }.total().cents
        val diff = total.cents - sum
        return when {
            diff > 0 -> "Faltan ${formatDiff(diff)}"
            diff < 0 -> "Sobran ${formatDiff(-diff)}"
            else -> null
        }
    }

    /** Mensaje de error si los montos asignados no suman el total (o no hay participantes); null si cuadra. */
    fun validateAmounts(total: Money, shares: List<SplitShare>): String? {
        if (shares.isEmpty()) return "Selecciona al menos una persona"
        val sum = shares.map { it.share }.total().cents
        val diff = total.cents - sum
        return when {
            diff > 0 -> "Faltan ${formatDiff(diff)}"
            diff < 0 -> "Sobran ${formatDiff(-diff)}"
            else -> null
        }
    }

    /** Mensaje de error si los porcentajes no suman 100 (o no hay participantes); null si cuadran. */
    fun validatePercents(percents: List<Pair<String, Int>>): String? {
        if (percents.isEmpty()) return "Selecciona al menos una persona"
        val sum = percents.sumOf { it.second }
        return when {
            sum < 100 -> "Suman $sum%"
            sum > 100 -> "Suman $sum%"
            else -> null
        }
    }

    private fun formatDiff(cents: Long): String {
        val major = cents / 100.0
        return "$" + if (major % 1.0 == 0.0) major.toLong().toString() else major.toString()
    }
}
