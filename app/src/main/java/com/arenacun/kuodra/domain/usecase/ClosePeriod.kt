package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.PeriodLine
import com.arenacun.kuodra.domain.model.PeriodSnapshot
import com.arenacun.kuodra.domain.model.newId
import com.arenacun.kuodra.domain.model.total
import java.time.LocalDate
import java.time.YearMonth

/**
 * Cierre manual de un periodo: agrega los movimientos de la ventana vigente por categoría y congela
 * un [PeriodSnapshot]. Si hay presupuesto activo usa su ventana ([BudgetPeriod]); si no, el mes en
 * curso. Puro y testeable.
 */
object ClosePeriod {

    fun build(
        budget: BudgetConfig?,
        movements: List<Movement>,
        categories: Map<String, Category>,
        today: LocalDate,
    ): PeriodSnapshot {
        val active = budget?.takeIf { it.enabled }
        val window = if (active != null) BudgetPeriod.current(active, today) else monthWindow(today)
        val inWindow = movements.filter { window.contains(it.date) }
        val total = inWindow.map { it.amount }.total()
        val lines = inWindow.groupBy { it.categoryId }
            .map { (id, list) ->
                val category = categories[id] ?: Category.byId(id)
                PeriodLine(category.name, list.size, list.map { it.amount }.total(), category.tone)
            }
            .sortedByDescending { it.amount.cents }
        val budgetAmount = active?.let { Calc.parseAmount(it.amount)?.let(Money::ofMajor) }
        return PeriodSnapshot(
            id = newId(),
            title = "${DateLabels.dayMonth(window.start)} – ${DateLabels.dayMonth(window.end)}",
            periodStart = window.start,
            periodEnd = window.end,
            totalSpent = total,
            budgetAmount = budgetAmount,
            lines = lines,
            createdAt = System.currentTimeMillis(),
        )
    }

    private fun monthWindow(today: LocalDate): BudgetWindow {
        val ym = YearMonth.from(today)
        return BudgetWindow(ym.atDay(1), ym.atEndOfMonth())
    }
}
