package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MovementQueryTest {

    private val today = LocalDate.of(2026, 6, 20)

    /** El `categoryId` lleva el nombre de categoría para que el resolutor sea la identidad. */
    private fun mov(id: String, title: String, cat: String, by: String?, date: LocalDate) =
        Movement(id, Money.ofMajor(10.0), cat, title, payer = by, date = date)

    private val data = listOf(
        mov("a", "Súper de la semana", "Súper", "Tú", today),
        mov("b", "Uber al centro", "Transporte", "Andrea", today.minusDays(1)),
        mov("c", "Internet", "Servicios", "Beto", today.minusDays(20)),
    )

    private val name: (Movement) -> String = { it.categoryId }

    @Test
    fun `query matches title and category case-insensitively`() {
        val r = MovementQuery.filter(data, MovementFilter(query = "uber"), today, name)
        assertEquals(listOf("b"), r.map { it.id })
    }

    @Test
    fun `category filter keeps only selected categories`() {
        val r = MovementQuery.filter(data, MovementFilter(categories = setOf("Súper", "Servicios")), today, name)
        assertEquals(setOf("a", "c"), r.map { it.id }.toSet())
    }

    @Test
    fun `responsible filter keeps only selected people`() {
        val r = MovementQuery.filter(data, MovementFilter(responsibles = setOf("Andrea")), today, name)
        assertEquals(listOf("b"), r.map { it.id })
    }

    @Test
    fun `this week period excludes older movements`() {
        val r = MovementQuery.filter(data, MovementFilter(period = MovementPeriod.ThisWeek), today, name)
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
