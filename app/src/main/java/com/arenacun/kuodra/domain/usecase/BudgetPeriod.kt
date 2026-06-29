package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Ventana (inclusiva en ambos extremos) de un periodo de presupuesto. */
data class BudgetWindow(val start: LocalDate, val end: LocalDate) {

    fun contains(date: LocalDate): Boolean = !date.isBefore(start) && !date.isAfter(end)

    /** Fracción del periodo transcurrida a [today] (0f..1f). */
    fun elapsedFraction(today: LocalDate): Float {
        val total = (end.toEpochDay() - start.toEpochDay() + 1).toFloat()
        if (total <= 0f) return 1f
        val done = (today.toEpochDay() - start.toEpochDay() + 1).toFloat()
        return (done / total).coerceIn(0f, 1f)
    }
}

/**
 * Calcula el periodo de presupuesto vigente a una fecha, según la frecuencia configurada. Puro y
 * testeable. El presupuesto es una **ventana de tiempo recurrente**: todos los movimientos con
 * fecha dentro de la ventana cuentan, sin importar cuándo se capturaron.
 */
object BudgetPeriod {

    fun current(config: BudgetConfig, today: LocalDate): BudgetWindow = when (config.frequency) {
        BudgetFrequency.Weekly -> weekly(config.weekday, today)
        BudgetFrequency.Biweekly -> biweekly(config.firstDay, config.secondDay, today)
        BudgetFrequency.Monthly -> monthly(config.monthlyDay, today)
        BudgetFrequency.Custom -> custom(config.customInterval, today)
    }

    /** Semana que empieza en [weekday] (1 = lunes … 7 = domingo). */
    private fun weekly(weekday: Int, today: LocalDate): BudgetWindow {
        val diff = ((today.dayOfWeek.value - weekday + 7) % 7).toLong()
        val start = today.minusDays(diff)
        return BudgetWindow(start, start.plusDays(6))
    }

    /** Quincenal: ventanas [first..second-1] y [second..nextFirst-1]. */
    private fun biweekly(firstDay: Int, secondDay: Int, today: LocalDate): BudgetWindow {
        val ym = YearMonth.from(today)
        val first = dateOf(ym, firstDay)
        val second = dateOf(ym, secondDay)
        return when {
            !today.isBefore(second) -> BudgetWindow(second, dateOf(ym.plusMonths(1), firstDay).minusDays(1))
            !today.isBefore(first) -> BudgetWindow(first, second.minusDays(1))
            else -> BudgetWindow(dateOf(ym.minusMonths(1), secondDay), first.minusDays(1))
        }
    }

    /** Mensual: del día ancla al día anterior del mes siguiente. */
    private fun monthly(day: Int, today: LocalDate): BudgetWindow {
        val ym = YearMonth.from(today)
        val anchor = dateOf(ym, day)
        return if (!today.isBefore(anchor)) {
            BudgetWindow(anchor, dateOf(ym.plusMonths(1), day).minusDays(1))
        } else {
            BudgetWindow(dateOf(ym.minusMonths(1), day), anchor.minusDays(1))
        }
    }

    /**
     * Personalizado: ciclos de N días anclados al inicio del año en curso (aproximado hasta que se
     * guarde una fecha de inicio real del presupuesto en Fase 3).
     */
    private fun custom(interval: Int, today: LocalDate): BudgetWindow {
        val n = interval.coerceAtLeast(1).toLong()
        val anchor = LocalDate.of(today.year, 1, 1)
        val k = ChronoUnit.DAYS.between(anchor, today) / n
        val start = anchor.plusDays(k * n)
        return BudgetWindow(start, start.plusDays(n - 1))
    }

    /** Día del mes acotado a la longitud real del mes (p. ej. 31 → 28/29 en febrero). */
    private fun dateOf(ym: YearMonth, day: Int): LocalDate = ym.atDay(day.coerceIn(1, ym.lengthOfMonth()))
}
