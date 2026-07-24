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
import java.time.LocalDate

class CloseSettlementTest {

    private val today = LocalDate.of(2026, 6, 20)
    private val persons = mapOf("a" to SpacePerson("a", "Andrea", "+521"))

    private fun expense(id: String, spaceId: String, total: Long, settlementId: String = "") = Movement(
        id = id,
        amount = Money(total),
        categoryId = "otro",
        title = "t",
        spaceId = spaceId,
        payers = listOf(PayerShare(PersonRef.ME, Money(total))),
        splits = listOf(SplitShare(PersonRef.ME, Money(total / 2)), SplitShare("a", Money(total / 2))),
        settlementId = settlementId,
    )

    @Test
    fun `build freezes live balances and returns the ids to stamp`() {
        val movements = listOf(
            expense("m1", "s1", 1000),
            expense("m2", "s1", 500, settlementId = "old"), // ya liquidado: se ignora
            expense("m3", "s2", 800),                       // otro espacio: se ignora
        )

        val result = CloseSettlement.build("s1", "Casa Roma", movements, persons, today)

        assertEquals(listOf("m1"), result.movementIds)
        assertEquals(Money(1000), result.settlement.total)
        assertEquals("Liquidación de junio", result.settlement.title)
        // Neto: yo +500, Andrea −500.
        val me = result.settlement.lines.first { it.personId == PersonRef.ME }
        val andrea = result.settlement.lines.first { it.personId == "a" }
        assertEquals(500L, me.net.cents)
        assertEquals("Tú", me.name)
        assertEquals(-500L, andrea.net.cents)
        assertEquals("Andrea", andrea.name)
        // Andrea le paga 500 a Tú.
        assertEquals(1, result.settlement.transfers.size)
        assertEquals("a", result.settlement.transfers[0].fromId)
        assertEquals(PersonRef.ME, result.settlement.transfers[0].toId)
    }

    @Test
    fun `deleted person keeps a placeholder name in the frozen line`() {
        val movements = listOf(expense("m1", "s1", 1000))
        val result = CloseSettlement.build("s1", "Casa Roma", movements, emptyMap(), today)
        assertTrue(result.settlement.lines.any { it.personId == "a" && it.name == "(eliminado)" })
    }
}
