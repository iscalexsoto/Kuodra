package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitRule
import com.arenacun.kuodra.domain.model.SplitRuleShare

/**
 * Saneado y validación de la **regla de división por defecto** de un espacio. Puro y testeable.
 * Complementa a [SplitCalc], que reparte el total de **un gasto concreto** a centavos exactos: aquí
 * solo se trabaja con configuración (porcentajes enteros que deben sumar 100).
 */
object SplitRuleCalc {

    /**
     * Deja la regla **siempre aplicable**, para que un gasto nunca quede bloqueado por una
     * configuración a medias: poda los ids que ya no existen (contacto borrado), coacciona el modo a
     * Equal/Percent, cae a [PersonRef.ME] si el pagador desapareció y, en modo Percent, reescala los
     * porcentajes a 100 (todos en cero ⇒ reparto parejo). Sin participantes queda deshabilitada.
     * Idempotente: `sanitize(sanitize(r)) == sanitize(r)`.
     */
    fun sanitize(rule: SplitRule, memberIds: List<String>): SplitRule {
        val kept = rule.shares.filter { it.personId in memberIds }.distinctBy { it.personId }
        if (kept.isEmpty()) return rule.copy(enabled = false, shares = emptyList())
        val mode = if (rule.mode == SplitMode.Percent) SplitMode.Percent else SplitMode.Equal
        val shares = when {
            mode != SplitMode.Percent -> kept
            kept.sumOf { it.percent } <= 0 -> evenPercents(kept.map { it.personId })
            else -> scaleToHundred(kept)
        }
        return rule.copy(
            mode = mode,
            shares = shares,
            payerId = if (rule.payerId in memberIds) rule.payerId else PersonRef.ME,
        )
    }

    /**
     * Regla implícita de un espacio **sin** regla configurada: reproduce la heurística histórica del
     * alta — con pocos miembros (≤2) participan todos por comodidad; con más, solo "Tú" y el usuario
     * elige a quién añadir (así el equitativo es realmente entre los que participaron).
     */
    fun implicitDefault(memberIds: List<String>): SplitRule {
        val ids = if (memberIds.size > 2) listOf(PersonRef.ME) else memberIds
        return SplitRule(
            enabled = false,
            mode = SplitMode.Equal,
            shares = ids.map { SplitRuleShare(it) },
            payerId = PersonRef.ME,
        )
    }

    /**
     * Mensaje para la UI de Ajustes si la regla está incompleta (null = cuadra). Delega en
     * [SplitCalc.validatePercents] para que los textos sean los mismos que en la pantalla de división.
     */
    fun validate(rule: SplitRule, memberIds: List<String>): String? {
        val kept = rule.shares.filter { it.personId in memberIds }
        if (kept.isEmpty()) return "Selecciona al menos una persona"
        return if (rule.mode == SplitMode.Percent)
            SplitCalc.validatePercents(kept.map { it.personId to it.percent }) else null
    }

    /** Reparto parejo en enteros que suman 100; el remanente va a los primeros ids (3 ⇒ 34/33/33). */
    fun evenPercents(ids: List<String>): List<SplitRuleShare> {
        if (ids.isEmpty()) return emptyList()
        val base = 100 / ids.size
        val remainder = 100 - base * ids.size
        return ids.mapIndexed { i, id -> SplitRuleShare(id, base + if (i < remainder) 1 else 0) }
    }

    /** Reescala proporcionalmente a 100 (piso + remanente de a 1 en orden), como `resolvePercents`. */
    internal fun scaleToHundred(shares: List<SplitRuleShare>): List<SplitRuleShare> {
        val sum = shares.sumOf { it.percent }
        if (sum == 100) return shares
        val scaled = shares.map { it.personId to it.percent * 100 / sum }
        var remainder = 100 - scaled.sumOf { it.second }
        return scaled.map { (id, pct) ->
            val extra = if (remainder > 0) 1 else 0
            remainder -= extra
            SplitRuleShare(id, pct + extra)
        }
    }
}
