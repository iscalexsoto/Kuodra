package com.arenacun.kuodra.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class MovementItemTest {

    private fun item(cents: Long) = MovementItem(id = "i", concept = "x", amount = Money(cents))

    @Test
    fun `adjustment equals total when there are no items`() {
        assertEquals(Money(124_000), adjustmentOf(Money(124_000), emptyList()))
    }

    @Test
    fun `adjustment is total minus the sum of items`() {
        val items = listOf(item(40_000), item(60_000))
        assertEquals(Money(24_000), adjustmentOf(Money(124_000), items))
    }

    @Test
    fun `adjustment is negative when items exceed the total`() {
        val items = listOf(item(80_000), item(60_000))
        assertEquals(Money(-16_000), adjustmentOf(Money(124_000), items))
    }

    @Test
    fun `adjustment is zero when items cover the total exactly`() {
        val items = listOf(item(100_000), item(24_000))
        assertEquals(Money.Zero, adjustmentOf(Money(124_000), items))
    }
}
