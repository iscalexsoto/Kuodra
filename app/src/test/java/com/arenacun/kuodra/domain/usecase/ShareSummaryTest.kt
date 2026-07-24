package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SplitShare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareSummaryTest {

    private val contacts = mapOf(
        "a" to SpacePerson("a", "Andrea"),
        "b" to SpacePerson("b", "Beto"),
    )

    private fun expense(payers: List<PayerShare>, splits: List<SplitShare>) = Movement(
        id = "m1", amount = Money(splits.sumOf { it.share.cents }), categoryId = "otro",
        title = "t", spaceId = "s1", payers = payers, splits = splits,
    )

    @Test
    fun `builds names, balances and suggested transfers`() {
        // Tú pagas 900, dividido en 3 (300 c/u): tú +600, Andrea y Beto −300.
        val movement = expense(
            payers = listOf(PayerShare(PersonRef.ME, Money(900))),
            splits = listOf(
                SplitShare(PersonRef.ME, Money(300)),
                SplitShare("a", Money(300)),
                SplitShare("b", Money(300)),
            ),
        )
        val text = ShareSummary.build("Casa Roma", listOf(movement), contacts)

        assertTrue(text.contains("Casa Roma"))
        assertTrue(text.contains("Tú"))
        assertTrue(text.contains("Andrea"))
        assertTrue(text.contains("Beto"))
        assertTrue(text.contains("Para saldar:"))
        // Andrea y Beto le transfieren a Tú.
        assertTrue(text.contains("Andrea → Tú"))
        assertTrue(text.contains("Beto → Tú"))
    }

    @Test
    fun `no pending balances yields a settled message`() {
        val text = ShareSummary.build("Casa Roma", emptyList(), contacts)
        assertEquals(true, text.contains("Todo está saldado"))
        assertTrue(!text.contains("Para saldar:"))
    }
}
