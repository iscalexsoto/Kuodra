package com.arenacun.kuodra.domain.model

import java.time.LocalDate

/**
 * Movimiento (gasto/ingreso). Modelo **data-shaped**, persistible y sincronizable: el monto es
 * numérico ([Money]) y la categoría es una **referencia** ([categoryId]). Los textos de
 * presentación (monto con formato, meta, fecha legible, perHead, verbo) NO se guardan: se derivan
 * en la capa de presentación (`MovementUi`).
 */
data class Movement(
    val id: String,
    val amount: Money,
    val categoryId: String,
    val title: String,
    val note: String = "",
    /** Fecha real del movimiento (para agrupar/filtrar y para los periodos de presupuesto). */
    val date: LocalDate = LocalDate.now(),
    /** Quién pagó/reportó (Gastos/Caja); null en Personal. */
    val payer: String? = null,
    /** Personas entre las que se divide el gasto (Gastos). */
    val splitNames: List<String> = emptyList(),
)

/** Inicial(es) a partir de un nombre. */
fun initialsOf(name: String): String = name.trim().take(1).uppercase()

/** Tono de avatar determinístico por nombre conocido. */
fun toneForName(name: String): AvatarTone = when (name) {
    "Tú" -> AvatarTone.Tint
    "Andrea", "Luis" -> AvatarTone.Tint
    "Caro", "Mar" -> AvatarTone.Pos
    "Diego", "Sol" -> AvatarTone.Warn
    "Beto" -> AvatarTone.Neg
    else -> AvatarTone.Tint
}
