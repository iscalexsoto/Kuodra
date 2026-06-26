package com.arenacun.kuodra.domain.model

import java.time.LocalDate

/**
 * Lógica pura del calendario (`reference/Calendar.dc.html`): construye la rejilla de un mes
 * con domingo como primer día, deshabilitando fechas futuras. Vive en `domain` (usa
 * `java.time`, no Android) para testearse en host. La UI (`KuodraCalendar`) solo dibuja y
 * lleva el (año, mes) visible, recomputando con [CalendarMonth.of] al navegar.
 */
data class CalendarCell(
    /** `null` = celda en blanco antes del día 1; si no, el día del mes. */
    val date: LocalDate?,
    val enabled: Boolean,
)

data class CalendarMonth(
    val year: Int,
    /** Mes 1..12. */
    val month: Int,
    val title: String,
    val cells: List<CalendarCell>,
    /** Si se permite avanzar al mes siguiente (acotado a "hoy"). */
    val canGoNext: Boolean,
) {
    companion object {
        private val MONTHS = listOf(
            "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
            "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
        )

        /** Etiquetas de día (domingo primero), como en el prototipo. */
        val WEEKDAYS = listOf("D", "L", "M", "M", "J", "V", "S")

        fun of(year: Int, month: Int, today: LocalDate): CalendarMonth {
            val firstOfMonth = LocalDate.of(year, month, 1)
            // getDay() del prototipo: domingo=0 … sábado=6
            val leadingBlanks = firstOfMonth.dayOfWeek.value % 7
            val daysInMonth = firstOfMonth.lengthOfMonth()

            val cells = buildList {
                repeat(leadingBlanks) { add(CalendarCell(null, enabled = false)) }
                for (day in 1..daysInMonth) {
                    val date = LocalDate.of(year, month, day)
                    add(CalendarCell(date, enabled = !date.isAfter(today)))
                }
            }

            // atCurrent: no se navega más allá del mes de "hoy".
            val atCurrent = year > today.year || (year == today.year && month >= today.monthValue)

            return CalendarMonth(
                year = year,
                month = month,
                title = "${MONTHS[month - 1]} $year",
                cells = cells,
                canGoNext = !atCurrent,
            )
        }

        /** Mes anterior al dado (recomputado contra [today]). */
        fun prev(current: CalendarMonth, today: LocalDate): CalendarMonth {
            val d = LocalDate.of(current.year, current.month, 1).minusMonths(1)
            return of(d.year, d.monthValue, today)
        }

        /** Mes siguiente, acotado a "hoy" (si no se puede, devuelve el mismo). */
        fun next(current: CalendarMonth, today: LocalDate): CalendarMonth {
            if (!current.canGoNext) return current
            val d = LocalDate.of(current.year, current.month, 1).plusMonths(1)
            return of(d.year, d.monthValue, today)
        }

        /** Mes que contiene a [selected] (o a [today] si es null). */
        fun forSelection(selected: LocalDate?, today: LocalDate): CalendarMonth {
            val anchor = selected ?: today
            return of(anchor.year, anchor.monthValue, today)
        }
    }
}
