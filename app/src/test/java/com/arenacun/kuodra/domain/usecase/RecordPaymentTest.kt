package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.SettlementKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecordPaymentTest {

    private val today = LocalDate.of(2026, 7, 24)

    @Test
    fun `debtor paying you builds a person to me transfer`() {
        // Andrea te debe 500 (net −500) y te paga 200 (parcial).
        val payment = RecordPayment.build("s1", "a", "Andrea", Money(200), currentNet = -500, today = today)
        assertEquals(SettlementKind.Payment, payment.kind)
        assertEquals("Pago de Andrea", payment.title)
        assertEquals(Money(200), payment.total)
        val t = payment.transfers.single()
        assertEquals("a", t.fromId)
        assertEquals(PersonRef.ME, t.toId)
        assertEquals(200L, t.amount.cents)
    }

    @Test
    fun `paying a creditor builds a me to person transfer`() {
        // Le debes 300 a Beto (net +300) y le pagas 300.
        val payment = RecordPayment.build("s1", "b", "Beto", Money(300), currentNet = 300, today = today)
        val t = payment.transfers.single()
        assertEquals(PersonRef.ME, t.fromId)
        assertEquals("b", t.toId)
        assertEquals(300L, t.amount.cents)
    }

    @Test
    fun `amount is capped to the outstanding balance`() {
        // Debe 500 pero intentas cobrar 900: se acota a 500.
        val payment = RecordPayment.build("s1", "a", "Andrea", Money(900), currentNet = -500, today = today)
        assertEquals(500L, payment.total.cents)
        assertEquals(500L, payment.transfers.single().amount.cents)
    }

    @Test
    fun `a payment transfer credits the person and debits me`() {
        val payment = RecordPayment.build("s1", "a", "Andrea", Money(500), currentNet = -500, today = today)
        // Aislado (sin la deuda de movimientos): el pago aporta +500 a Andrea y −500 a Tú.
        // Combinado con la deuda −500 la deja en cero (ver SharedBalancesTest).
        val balances = SharedBalances.compute(emptyList(), listOf(payment))
        assertEquals(500L, balances["a"])
        assertEquals(-500L, balances[PersonRef.ME])
    }
}
