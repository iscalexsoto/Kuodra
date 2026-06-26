package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Movement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MovementQueryTest {

    private val today = LocalDate.of(2026, 6, 20)

    private fun mov(id: String, title: String, cat: String, by: String?, date: LocalDate) =
        Movement(id, title, "meta", "$10", cat.take(2), cat, AvatarTone.Tint, "x", by = by, date = date)

    private val data = listOf(
        mov("a", "Súper de la semana", "Súper", "Tú", today),
        mov("b", "Uber al centro", "Transporte", "Andrea", today.minusDays(1)),
        mov("c", "Internet", "Servicios", "Beto", today.minusDays(20)),
    )

    @Test
    fun `query matches title and category case-insensitively`() {
        val r = MovementQuery.filter(data, MovementFilter(query = "uber"), today)
        assertEquals(listOf("b"), r.map { it.id })
    }

    @Test
    fun `category filter keeps only selected categories`() {
        val r = MovementQuery.filter(data, MovementFilter(categories = setOf("Súper", "Servicios")), today)
        assertEquals(setOf("a", "c"), r.map { it.id }.toSet())
    }

    @Test
    fun `responsible filter keeps only selected people`() {
        val r = MovementQuery.filter(data, MovementFilter(responsibles = setOf("Andrea")), today)
        assertEquals(listOf("b"), r.map { it.id })
    }

    @Test
    fun `this week period excludes older movements`() {
        val r = MovementQuery.filter(data, MovementFilter(period = MovementPeriod.ThisWeek), today)
        assertEquals(setOf("a", "b"), r.map { it.id }.toSet())
    }

    @Test
    fun `groups by day in descending order with headers`() {
        val groups = MovementQuery.groupByDay(data, today)
        assertEquals(3, groups.size)
        assertTrue(groups.first().header.startsWith("Hoy"))
        assertEquals(listOf("a", "b", "c"), groups.flatMap { g -> g.movements.map { it.id } })
    }
}
