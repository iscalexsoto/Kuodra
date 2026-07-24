package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.SplitShare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SplitCalcTest {

    @Test
    fun `equal split sums to total and gives remainder cents to the first ids`() {
        val shares = SplitCalc.resolveEqual(Money(1000), listOf("a", "b", "c"))
        assertEquals(listOf(334L, 333L, 333L), shares.map { it.share.cents })
        assertEquals(1000L, shares.sumOf { it.share.cents })
    }

    @Test
    fun `equal split with exact division has no remainder`() {
        val shares = SplitCalc.resolveEqual(Money(900), listOf("a", "b", "c"))
        assertEquals(listOf(300L, 300L, 300L), shares.map { it.share.cents })
    }

    @Test
    fun `percent split sums to total with deterministic remainder`() {
        // 33% + 33% + 34% of 1000 = 330 + 330 + 340 = 1000, no remainder.
        val shares = SplitCalc.resolvePercents(Money(1000), listOf("a" to 33, "b" to 33, "c" to 34))
        assertEquals(listOf(330L, 330L, 340L), shares.map { it.share.cents })
        assertEquals(1000L, shares.sumOf { it.share.cents })
    }

    @Test
    fun `percent split distributes rounding remainder by order`() {
        // 333 = 33%+33%+34% → 109.89 + 109.89 + 113.22 → floor 109,109,113 sum=331, remainder 2 → first two get +1.
        val shares = SplitCalc.resolvePercents(Money(333), listOf("a" to 33, "b" to 33, "c" to 34))
        assertEquals(333L, shares.sumOf { it.share.cents })
        assertEquals(listOf(110L, 110L, 113L), shares.map { it.share.cents })
    }

    @Test
    fun `validatePayers detects shortfall surplus and match`() {
        val total = Money(1000)
        assertNotNull(SplitCalc.validatePayers(total, emptyList()))
        assertNotNull(SplitCalc.validatePayers(total, listOf(PayerShare("a", Money(800)))))
        assertNotNull(SplitCalc.validatePayers(total, listOf(PayerShare("a", Money(1200)))))
        assertNull(SplitCalc.validatePayers(total, listOf(PayerShare("a", Money(600)), PayerShare("b", Money(400)))))
    }

    @Test
    fun `validateAmounts requires exact total`() {
        val total = Money(1000)
        assertNotNull(SplitCalc.validateAmounts(total, emptyList()))
        assertNotNull(SplitCalc.validateAmounts(total, listOf(SplitShare("a", Money(900)))))
        assertNull(SplitCalc.validateAmounts(total, listOf(SplitShare("a", Money(500)), SplitShare("b", Money(500)))))
    }

    @Test
    fun `validatePercents requires sum of one hundred`() {
        assertNotNull(SplitCalc.validatePercents(emptyList()))
        assertNotNull(SplitCalc.validatePercents(listOf("a" to 50, "b" to 40)))
        assertNotNull(SplitCalc.validatePercents(listOf("a" to 60, "b" to 60)))
        assertNull(SplitCalc.validatePercents(listOf("a" to 50, "b" to 50)))
    }
}
