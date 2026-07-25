package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.SplitShare
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class SharedBalancesTest {

    /** Fecha fija: no interviene en los saldos y el reloj real solo aportaría fragilidad. */
    private val today: LocalDate = LocalDate.of(2026, 6, 20)

    private fun expense(
        id: String,
        total: Long,
        payers: List<PayerShare>,
        splits: List<SplitShare>,
        settlementId: String = "",
    ) = Movement(
        id = id,
        amount = Money(total),
        categoryId = "otro",
        title = "t",
        spaceId = "s1",
        payers = payers,
        splits = splits,
        settlementId = settlementId,
    )

    @Test
    fun `single payer equal split leaves payer credited and others in debt`() {
        // Yo pago 900, dividido en 3 (300 c/u): yo neto +600, a y b −300.
        val movement = expense(
            "m1", 900,
            payers = listOf(PayerShare(PersonRef.ME, Money(900))),
            splits = listOf(SplitShare(PersonRef.ME, Money(300)), SplitShare("a", Money(300)), SplitShare("b", Money(300))),
        )
        val balances = SharedBalances.compute(listOf(movement))
        assertEquals(600L, balances[PersonRef.ME])
        assertEquals(-300L, balances["a"])
        assertEquals(-300L, balances["b"])
    }

    @Test
    fun `multiple payers are each credited their contribution`() {
        // Pagan yo 600 y a 400 (total 1000), dividido en 4 de 250: yo +350, a +150, b −250, c −250.
        val movement = expense(
            "m1", 1000,
            payers = listOf(PayerShare(PersonRef.ME, Money(600)), PayerShare("a", Money(400))),
            splits = listOf(
                SplitShare(PersonRef.ME, Money(250)), SplitShare("a", Money(250)),
                SplitShare("b", Money(250)), SplitShare("c", Money(250)),
            ),
        )
        val balances = SharedBalances.compute(listOf(movement))
        assertEquals(350L, balances[PersonRef.ME])
        assertEquals(150L, balances["a"])
        assertEquals(-250L, balances["b"])
        assertEquals(-250L, balances["c"])
        assertEquals(0L, balances.values.sum())
    }

    @Test
    fun `payer not in the split is fully credited`() {
        // a paga 500 pero no participa; se divide entre b y c: a +500, b −250, c −250.
        val movement = expense(
            "m1", 500,
            payers = listOf(PayerShare("a", Money(500))),
            splits = listOf(SplitShare("b", Money(250)), SplitShare("c", Money(250))),
        )
        val balances = SharedBalances.compute(listOf(movement))
        assertEquals(500L, balances["a"])
        assertEquals(-250L, balances["b"])
        assertEquals(-250L, balances["c"])
    }

    @Test
    fun `a live payment offsets the person and me`() {
        // Yo pago 900 dividido con "a" (450/450): yo +450, a −450.
        val movement = expense(
            "m1", 900,
            payers = listOf(PayerShare(PersonRef.ME, Money(900))),
            splits = listOf(SplitShare(PersonRef.ME, Money(450)), SplitShare("a", Money(450))),
        )
        // "a" te paga 450 (Transfer a → Tú): ambos a cero.
        val payment = RecordPayment.build("s1", "a", "Andrea", Money(450), currentNet = -450, today = today)
        val balances = SharedBalances.compute(listOf(movement), listOf(payment))
        assertEquals(null, balances["a"])
        assertEquals(null, balances[PersonRef.ME])
    }

    @Test
    fun `a consumed payment (settledBy set) no longer affects balances`() {
        val movement = expense(
            "m1", 900,
            payers = listOf(PayerShare(PersonRef.ME, Money(900))),
            splits = listOf(SplitShare(PersonRef.ME, Money(450)), SplitShare("a", Money(450))),
        )
        val consumed = RecordPayment.build("s1", "a", "Andrea", Money(450), currentNet = -450, today = today)
            .copy(settledBy = "corte1")
        val balances = SharedBalances.compute(listOf(movement), listOf(consumed))
        // El pago consumido se ignora: saldos originales.
        assertEquals(450L, balances[PersonRef.ME])
        assertEquals(-450L, balances["a"])
    }

    @Test
    fun `settled movements are ignored`() {
        val live = expense(
            "m1", 900,
            payers = listOf(PayerShare(PersonRef.ME, Money(900))),
            splits = listOf(SplitShare("a", Money(900))),
        )
        val settled = expense(
            "m2", 1000,
            payers = listOf(PayerShare("a", Money(1000))),
            splits = listOf(SplitShare(PersonRef.ME, Money(1000))),
            settlementId = "old",
        )
        val balances = SharedBalances.compute(listOf(live, settled))
        assertEquals(900L, balances[PersonRef.ME])
        assertEquals(-900L, balances["a"])
    }
}
