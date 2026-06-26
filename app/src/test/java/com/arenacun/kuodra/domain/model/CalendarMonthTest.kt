package com.arenacun.kuodra.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CalendarMonthTest {

    private val today = LocalDate.of(2026, 6, 20)

    @Test
    fun `builds all days of the month`() {
        val june = CalendarMonth.of(2026, 6, today)
        assertEquals(30, june.cells.count { it.date != null })
        assertEquals("Junio 2026", june.title)
    }

    @Test
    fun `leading blanks align day one to its weekday`() {
        val june = CalendarMonth.of(2026, 6, today)
        val leading = june.cells.takeWhile { it.date == null }.size
        // 1 jun 2026 cae en lunes → domingo=0 … lunes=1 celda en blanco
        assertEquals(LocalDate.of(2026, 6, 1).dayOfWeek.value % 7, leading)
    }

    @Test
    fun `future days are disabled`() {
        val june = CalendarMonth.of(2026, 6, today)
        val d20 = june.cells.first { it.date == today }
        val d21 = june.cells.first { it.date == today.plusDays(1) }
        assertTrue(d20.enabled)
        assertFalse(d21.enabled)
    }

    @Test
    fun `cannot navigate past the current month`() {
        val june = CalendarMonth.of(2026, 6, today)
        assertFalse(june.canGoNext)
    }

    @Test
    fun `past months can navigate forward`() {
        val may = CalendarMonth.prev(CalendarMonth.of(2026, 6, today), today)
        assertEquals("Mayo 2026", may.title)
        assertTrue(may.canGoNext)
    }

    @Test
    fun `next is bounded and clamps at current month`() {
        val june = CalendarMonth.of(2026, 6, today)
        assertEquals(june, CalendarMonth.next(june, today))
    }

    @Test
    fun `selection anchors to its month`() {
        val m = CalendarMonth.forSelection(LocalDate.of(2026, 3, 14), today)
        assertEquals(3, m.month)
        assertEquals("Marzo 2026", m.title)
    }
}
