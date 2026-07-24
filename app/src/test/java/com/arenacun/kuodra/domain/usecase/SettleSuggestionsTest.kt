package com.arenacun.kuodra.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettleSuggestionsTest {

    @Test
    fun `one debtor pays one creditor the full amount`() {
        val transfers = SettleSuggestions.compute(mapOf("me" to 300L, "a" to -300L))
        assertEquals(1, transfers.size)
        assertEquals("a", transfers[0].fromId)
        assertEquals("me", transfers[0].toId)
        assertEquals(300L, transfers[0].amount.cents)
    }

    @Test
    fun `greedy pairs largest debtor with largest creditor`() {
        // Acreedores: me +600, b +150 ; deudores: c −500, d −250. me cobra 500 de c y 100 de d; b cobra 150 de d.
        val transfers = SettleSuggestions.compute(
            mapOf("me" to 600L, "b" to 150L, "c" to -500L, "d" to -250L),
        )
        // Todo salda: la suma de lo que recibe cada acreedor iguala su crédito.
        val received = transfers.groupBy { it.toId }.mapValues { e -> e.value.sumOf { it.amount.cents } }
        assertEquals(600L, received["me"])
        assertEquals(150L, received["b"])
        val paid = transfers.groupBy { it.fromId }.mapValues { e -> e.value.sumOf { it.amount.cents } }
        assertEquals(500L, paid["c"])
        assertEquals(250L, paid["d"])
    }

    @Test
    fun `balanced group needs no transfers`() {
        assertTrue(SettleSuggestions.compute(emptyMap()).isEmpty())
    }

    @Test
    fun `result is deterministic across runs`() {
        val balances = mapOf("me" to 400L, "a" to 200L, "b" to -300L, "c" to -300L)
        val first = SettleSuggestions.compute(balances)
        val second = SettleSuggestions.compute(balances)
        assertEquals(first, second)
    }
}
