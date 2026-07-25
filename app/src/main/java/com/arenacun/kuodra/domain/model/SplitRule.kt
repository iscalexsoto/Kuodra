package com.arenacun.kuodra.domain.model

/** Participante de una [SplitRule] y su [percent] (solo se usa en [SplitMode.Percent]). */
data class SplitRuleShare(val personId: String, val percent: Int = 0)

/**
 * Regla de división **por defecto** de un espacio de Gastos: una sola por espacio. Existe para
 * acuerdos fijos ("mi hermano paga el 75% y yo el 25%"): todo gasto nuevo llega prellenado con ella y
 * el usuario solo entra a la pantalla de división cuando ese gasto es la excepción.
 *
 * Solo [SplitMode.Equal] y [SplitMode.Percent] son modos válidos (un monto absoluto no es reutilizable
 * entre gastos de totales distintos); `SplitRuleCalc.sanitize` coacciona el resto a [SplitMode.Equal].
 * La regla puede estar guardada **incompleta** (porcentajes que no suman 100) mientras el usuario la
 * edita en Ajustes; se sanea al aplicarla, nunca al escribirla.
 */
data class SplitRule(
    val enabled: Boolean = false,
    val mode: SplitMode = SplitMode.Equal,
    /** Participantes y su %. El orden es estable para el JSON, no decide el remanente de centavos. */
    val shares: List<SplitRuleShare> = emptyList(),
    /** Quién suele pagar ([PersonRef.ME] o id de `persons`). */
    val payerId: String = PersonRef.ME,
    /** Registrar tu parte en Personal al guardar, sin preguntar. */
    val autoPersonalCopy: Boolean = false,
) {
    val participantIds: List<String> get() = shares.map { it.personId }

    fun percentOf(id: String): Int = shares.firstOrNull { it.personId == id }?.percent ?: 0

    companion object {
        /** Regla de un espacio nuevo: apagada, así que el alta se comporta como si no existiera. */
        val Default = SplitRule()
    }
}
