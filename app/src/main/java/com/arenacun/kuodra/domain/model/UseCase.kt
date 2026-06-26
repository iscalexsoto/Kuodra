package com.arenacun.kuodra.domain.model

import kotlinx.serialization.Serializable

/** Caso de uso del espacio: cambia el contenido y la terminología, no el styling. */
@Serializable
enum class UseCase { Personal, Gastos, Caja }

/** Terminología por caso de uso (objeto `t` del prototipo). */
data class Terminology(
    val groupName: String,
    val containerKind: String,
    val roleLabel: String,
    val heroLabel: String,
    val addTitle: String,
    val paidLabel: String,
    val saveNoun: String,
    val settleTitle: String,
)

fun terminologyFor(useCase: UseCase): Terminology = when (useCase) {
    UseCase.Personal -> Terminology(
        groupName = "Mis gastos", containerKind = "Personal", roleLabel = "solo tú",
        heroLabel = "Gastado · Quincena 2 · 16–30 jun", addTitle = "Nuevo gasto",
        paidLabel = "", saveNoun = "gasto", settleTitle = "",
    )
    UseCase.Gastos -> Terminology(
        groupName = "Casa Roma", containerKind = "Grupo", roleLabel = "4 miembros",
        heroLabel = "Tu balance del mes", addTitle = "Nuevo gasto",
        paidLabel = "¿Quién pagó?", saveNoun = "gasto", settleTitle = "Liquidación de junio",
    )
    UseCase.Caja -> Terminology(
        groupName = "Caja Changarro", containerKind = "Fondo", roleLabel = "Responsable de caja",
        heroLabel = "Fondo disponible", addTitle = "Nuevo movimiento",
        paidLabel = "¿Quién reportó?", saveNoun = "movimiento", settleTitle = "Corte de caja",
    )
}
