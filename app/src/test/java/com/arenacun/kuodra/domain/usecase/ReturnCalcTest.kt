package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.ReturnStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReturnCalcTest {

    private fun item(cents: Long, returnable: Boolean = true) =
        MovementItem(id = "i${cents}_$returnable", concept = "x", amount = Money(cents), returnable = returnable)

    private fun movement(
        cents: Long,
        status: ReturnStatus,
        items: List<MovementItem> = emptyList(),
        stamped: Int? = null,
    ) = Movement(
        id = "m$cents", amount = Money(cents), categoryId = "c", title = "t",
        items = items, returnStatus = status, returnPercent = stamped,
    )

    // ---- returnableBase ----

    @Test
    fun `base is the full total when there are no items`() {
        assertEquals(Money(90_00), ReturnCalc.returnableBase(Money(90_00), emptyList()))
    }

    @Test
    fun `base is the full total when every item is returnable`() {
        val items = listOf(item(60_00), item(30_00))
        assertEquals(Money(90_00), ReturnCalc.returnableBase(Money(90_00), items))
    }

    @Test
    fun `partial selection distributes the total proportionally including the discount`() {
        // total 90.00 but items only add to 100.00 (a global discount); A returnable, B not.
        val items = listOf(item(60_00, returnable = true), item(40_00, returnable = false))
        // 9000 * 6000/10000 = 5400
        assertEquals(Money(54_00), ReturnCalc.returnableBase(Money(90_00), items))
    }

    @Test
    fun `base is zero when nothing is selected`() {
        val items = listOf(item(60_00, returnable = false), item(40_00, returnable = false))
        assertEquals(Money.Zero, ReturnCalc.returnableBase(Money(90_00), items))
    }

    @Test
    fun `base is zero when items sum to zero`() {
        val items = listOf(item(0), item(0))
        assertEquals(Money.Zero, ReturnCalc.returnableBase(Money(90_00), items))
    }

    @Test
    fun `base rounds to the nearest cent`() {
        // total 100.01, items 0.01 + 0.02, only the 0.01 returnable → 10001 * 1/3 = 3333.67 → 3334
        val items = listOf(item(1, returnable = true), item(2, returnable = false))
        assertEquals(Money(3334), ReturnCalc.returnableBase(Money(100_01), items))
    }

    // ---- returnAmount (live) ----

    @Test
    fun `return amount applies the current percent to a pending movement`() {
        val m = movement(432_50, ReturnStatus.Pending)
        // 43250 * 75 / 100 = 32437.5 → 32438
        assertEquals(Money(324_38), ReturnCalc.returnAmount(m, 75))
    }

    @Test
    fun `return amount is zero for none and returned`() {
        assertEquals(Money.Zero, ReturnCalc.returnAmount(movement(100_00, ReturnStatus.None), 75))
        assertEquals(Money.Zero, ReturnCalc.returnAmount(movement(100_00, ReturnStatus.Returned, stamped = 50), 75))
    }

    @Test
    fun `return amount respects a non-default percent`() {
        assertEquals(Money(50_00), ReturnCalc.returnAmount(movement(100_00, ReturnStatus.Pending), 50))
    }

    // ---- returnedAmount (frozen) ----

    @Test
    fun `returned amount uses the stamped percent not the current one`() {
        val m = movement(100_00, ReturnStatus.Returned, stamped = 60)
        assertEquals(Money(60_00), ReturnCalc.returnedAmount(m))
    }

    @Test
    fun `returned amount falls back to default when unstamped`() {
        val m = movement(100_00, ReturnStatus.Returned, stamped = null)
        assertEquals(Money(75_00), ReturnCalc.returnedAmount(m))
    }

    @Test
    fun `returned amount is zero when not returned`() {
        assertEquals(Money.Zero, ReturnCalc.returnedAmount(movement(100_00, ReturnStatus.Pending)))
    }

    // ---- pendingTotal ----

    @Test
    fun `pending total sums only pending movements`() {
        val movements = listOf(
            movement(100_00, ReturnStatus.Pending),
            movement(200_00, ReturnStatus.Pending),
            movement(999_00, ReturnStatus.None),
            movement(999_00, ReturnStatus.Returned, stamped = 10),
        )
        // (10000 + 20000) * 75% = 22500
        assertEquals(Money(225_00), ReturnCalc.pendingTotal(movements, 75))
    }

    @Test
    fun `pending total is zero when there are no pending movements`() {
        assertEquals(Money.Zero, ReturnCalc.pendingTotal(listOf(movement(100_00, ReturnStatus.None)), 75))
    }
}
