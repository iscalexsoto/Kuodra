package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ClosePeriodTest {

    private val today = LocalDate.of(2026, 6, 20)

    private fun mov(id: String, category: String, amount: Double, date: LocalDate) =
        Movement(id, Money.ofMajor(amount), category, "t", date = date)

    @Test
    fun `snapshot aggregates by category within the budget window`() {
        val budget = BudgetConfig(true, BudgetFrequency.Biweekly, "$6,000", firstDay = 1, secondDay = 16)
        val movements = listOf(
            mov("a", "super", 100.0, LocalDate.of(2026, 6, 18)),   // dentro [16..30]
            mov("b", "super", 50.0, LocalDate.of(2026, 6, 19)),    // dentro
            mov("c", "transporte", 30.0, LocalDate.of(2026, 6, 17)), // dentro
            mov("d", "restaurantes", 999.0, LocalDate.of(2026, 6, 5)), // fuera (quincena anterior)
        )

        val snapshot = ClosePeriod.build(budget, movements, emptyMap(), today)

        assertEquals(LocalDate.of(2026, 6, 16), snapshot.periodStart)
        assertEquals(LocalDate.of(2026, 6, 30), snapshot.periodEnd)
        assertEquals(Money.ofMajor(180.0), snapshot.totalSpent)
        assertEquals(Money.ofMajor(6000.0), snapshot.budgetAmount)
        // Línea mayor primero: Súper $150 (2 mov), luego Transporte $30 (1 mov).
        assertEquals(listOf("Súper", "Transporte"), snapshot.lines.map { it.categoryName })
        assertEquals(2, snapshot.lines.first().count)
        assertEquals(Money.ofMajor(150.0), snapshot.lines.first().amount)
    }

    @Test
    fun `without budget uses the current month and no budget amount`() {
        val movements = listOf(
            mov("a", "super", 100.0, LocalDate.of(2026, 6, 2)),
            mov("b", "super", 50.0, LocalDate.of(2026, 6, 28)),
            mov("c", "super", 70.0, LocalDate.of(2026, 5, 30)), // mes anterior, fuera
        )

        val snapshot = ClosePeriod.build(budget = null, movements = movements, categories = emptyMap(), today = today)

        assertEquals(LocalDate.of(2026, 6, 1), snapshot.periodStart)
        assertEquals(LocalDate.of(2026, 6, 30), snapshot.periodEnd)
        assertEquals(Money.ofMajor(150.0), snapshot.totalSpent)
        assertNull(snapshot.budgetAmount)
    }

    @Test
    fun `disabled budget is treated as no budget`() {
        val budget = BudgetConfig(false, BudgetFrequency.Monthly, "$6,000", monthlyDay = 1)
        val snapshot = ClosePeriod.build(budget, emptyList(), emptyMap(), today)
        assertNull(snapshot.budgetAmount)
    }
}
