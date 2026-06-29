package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.Movement
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Periodo de filtrado de "Ver todo" (`vtOpenFilter` del prototipo). */
enum class MovementPeriod { All, ThisWeek, ThisMonth, LastMonth }

/** Criterios de búsqueda + filtros de la pantalla "Ver todo". */
data class MovementFilter(
    val query: String = "",
    /** Nombres de categoría seleccionados (vacío = todas). */
    val categories: Set<String> = emptySet(),
    val period: MovementPeriod = MovementPeriod.All,
    /** Responsables seleccionados por nombre (vacío = todos). */
    val responsibles: Set<String> = emptySet(),
) {
    val isActive: Boolean
        get() = query.isNotBlank() || categories.isNotEmpty() ||
            period != MovementPeriod.All || responsibles.isNotEmpty()
}

/** Grupo de movimientos bajo un encabezado de día. */
data class MovementGroup(val header: String, val movements: List<Movement>)

/**
 * Búsqueda, filtrado y agrupación de movimientos para "Ver todo" (`scrVerTodo`). Funciones
 * puras: sin Android, testeables en host. Como el [Movement] ya no guarda el nombre de categoría
 * (solo `categoryId`), [filter] recibe un resolutor [categoryName] para buscar/filtrar por nombre.
 */
object MovementQuery {

    fun filter(
        movements: List<Movement>,
        filter: MovementFilter,
        today: LocalDate,
        categoryName: (Movement) -> String,
    ): List<Movement> {
        val q = filter.query.trim().lowercase()
        return movements.filter { m ->
            (q.isEmpty() || matchesQuery(m, q, categoryName(m))) &&
                (filter.categories.isEmpty() || categoryName(m) in filter.categories) &&
                (filter.responsibles.isEmpty() || (m.payer != null && m.payer in filter.responsibles)) &&
                inPeriod(m.date, filter.period, today)
        }
    }

    /** Agrupa por día (descendente) con el encabezado del prototipo ("Hoy · 20 jun", …). */
    fun groupByDay(movements: List<Movement>, today: LocalDate): List<MovementGroup> =
        movements
            .sortedByDescending { it.date }
            .groupBy { it.date }
            .map { (date, list) -> MovementGroup(DateLabels.groupHeader(date, today), list) }

    private fun matchesQuery(m: Movement, q: String, catName: String): Boolean =
        m.title.lowercase().contains(q) ||
            catName.lowercase().contains(q) ||
            m.note.lowercase().contains(q) ||
            (m.payer?.lowercase()?.contains(q) ?: false)

    private fun inPeriod(date: LocalDate, period: MovementPeriod, today: LocalDate): Boolean = when (period) {
        MovementPeriod.All -> true
        MovementPeriod.ThisWeek -> ChronoUnit.DAYS.between(date, today) in 0..6
        MovementPeriod.ThisMonth -> date.year == today.year && date.month == today.month
        MovementPeriod.LastMonth -> {
            val lastMonth = today.minusMonths(1)
            date.year == lastMonth.year && date.month == lastMonth.month
        }
    }
}
