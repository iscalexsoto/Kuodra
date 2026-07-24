package com.arenacun.kuodra.domain.model

import androidx.annotation.Keep
import kotlinx.serialization.Serializable

/**
 * Caso de uso del espacio: cambia el contenido y la terminología, no el styling.
 * `@Keep` porque se usa como argumento type-safe de navegación: sin él, R8 podría ofuscar
 * los nombres de las constantes y romper la (de)serialización de la ruta en builds minificados.
 */
@Keep
@Serializable
enum class UseCase { Personal, Gastos }

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
}
