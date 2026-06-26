package com.arenacun.kuodra.domain.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Formateo puro de fechas con la terminología del prototipo ("Hoy · 20 jun 2026",
 * "Ayer · 19 jun", "Martes · 18 jun"). Sin Android: testeable y reutilizable por el alta de
 * movimiento y por la agrupación de "Ver todo".
 */
object DateLabels {

    private val MONTHS_SHORT = listOf(
        "ene", "feb", "mar", "abr", "may", "jun",
        "jul", "ago", "sep", "oct", "nov", "dic",
    )

    private val WEEKDAYS_LONG = listOf(
        "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo",
    )

    /** "20 jun" */
    fun dayMonth(date: LocalDate): String = "${date.dayOfMonth} ${MONTHS_SHORT[date.monthValue - 1]}"

    /** "20 jun 2026" */
    fun dayMonthYear(date: LocalDate): String = "${dayMonth(date)} ${date.year}"

    /** Etiqueta larga para el detalle/alta: "Hoy · 20 jun 2026", "Ayer · 19 jun 2026", "2 jun 2026". */
    fun longLabel(date: LocalDate, today: LocalDate): String = when (date) {
        today -> "Hoy · ${dayMonthYear(date)}"
        today.minusDays(1) -> "Ayer · ${dayMonthYear(date)}"
        else -> dayMonthYear(date)
    }

    /** Encabezado de grupo de "Ver todo": "Hoy · 20 jun", "Ayer · 19 jun", "Martes · 18 jun". */
    fun groupHeader(date: LocalDate, today: LocalDate): String = when {
        date == today -> "Hoy · ${dayMonth(date)}"
        date == today.minusDays(1) -> "Ayer · ${dayMonth(date)}"
        ChronoUnit.DAYS.between(date, today) in 2..6 ->
            "${WEEKDAYS_LONG[date.dayOfWeek.value - 1]} · ${dayMonth(date)}"
        else -> dayMonthYear(date)
    }
}
