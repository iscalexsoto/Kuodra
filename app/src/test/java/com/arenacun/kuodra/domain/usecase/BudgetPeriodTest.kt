package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BudgetPeriodTest {

    private fun budget(
        frequency: BudgetFrequency,
        weekday: Int = 1,
        firstDay: Int = 1,
        secondDay: Int = 16,
        monthlyDay: Int = 1,
        customInterval: Int = 15,
    ) = BudgetConfig(true, frequency, "$6,000", weekday, firstDay, secondDay, monthlyDay, customInterval)

    @Test
    fun `weekly window starts on the configured weekday`() {
        // 2026-06-20 es sábado; semana que empieza en lunes (1) ⇒ 15..21 jun.
        val w = BudgetPeriod.current(budget(BudgetFrequency.Weekly, weekday = 1), LocalDate.of(2026, 6, 20))
        assertEquals(LocalDate.of(2026, 6, 15), w.start)
        assertEquals(LocalDate.of(2026, 6, 21), w.end)
    }

    @Test
    fun `biweekly first and second windows`() {
        val b = budget(BudgetFrequency.Biweekly, firstDay = 1, secondDay = 16)
        val first = BudgetPeriod.current(b, LocalDate.of(2026, 6, 10))
        assertEquals(LocalDate.of(2026, 6, 1), first.start)
        assertEquals(LocalDate.of(2026, 6, 15), first.end)

        val second = BudgetPeriod.current(b, LocalDate.of(2026, 6, 20))
        assertEquals(LocalDate.of(2026, 6, 16), second.start)
        assertEquals(LocalDate.of(2026, 6, 30), second.end)
    }

    @Test
    fun `biweekly before first day falls in previous month second window`() {
        val b = budget(BudgetFrequency.Biweekly, firstDay = 5, secondDay = 20)
        val w = BudgetPeriod.current(b, LocalDate.of(2026, 6, 2))
        assertEquals(LocalDate.of(2026, 5, 20), w.start)
        assertEquals(LocalDate.of(2026, 6, 4), w.end)
    }

    @Test
    fun `monthly window spans from anchor to day before next anchor`() {
        val w = BudgetPeriod.current(budget(BudgetFrequency.Monthly, monthlyDay = 1), LocalDate.of(2026, 6, 15))
        assertEquals(LocalDate.of(2026, 6, 1), w.start)
        assertEquals(LocalDate.of(2026, 6, 30), w.end)
    }

    @Test
    fun `contains and elapsedFraction`() {
        val w = BudgetWindow(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 10)) // 10 días
        assertTrue(w.contains(LocalDate.of(2026, 6, 5)))
        assertFalse(w.contains(LocalDate.of(2026, 6, 11)))
        assertEquals(0.5f, w.elapsedFraction(LocalDate.of(2026, 6, 5)), 0.001f)
        assertEquals(1.0f, w.elapsedFraction(LocalDate.of(2026, 6, 10)), 0.001f)
    }
}
