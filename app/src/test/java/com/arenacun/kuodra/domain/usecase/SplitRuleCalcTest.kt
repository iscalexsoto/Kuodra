package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitRule
import com.arenacun.kuodra.domain.model.SplitRuleShare
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SplitRuleCalcTest {

    private val me = PersonRef.ME

    private fun percentRule(vararg shares: Pair<String, Int>) = SplitRule(
        enabled = true,
        mode = SplitMode.Percent,
        shares = shares.map { SplitRuleShare(it.first, it.second) },
    )

    // ---- sanitize ----

    @Test
    fun `sanitize keeps a valid rule untouched`() {
        val rule = percentRule(me to 25, "hermano" to 75)

        assertEquals(rule, SplitRuleCalc.sanitize(rule, listOf(me, "hermano")))
    }

    @Test
    fun `sanitize prunes a deleted contact and rescales the rest to a hundred`() {
        // El caso motivador: acuerdo 75/25 y se borra al hermano ⇒ queda "Tú" con el 100%.
        val rule = percentRule(me to 25, "hermano" to 75)

        val sanitized = SplitRuleCalc.sanitize(rule, listOf(me))

        assertEquals(listOf(SplitRuleShare(me, 100)), sanitized.shares)
    }

    @Test
    fun `sanitize disables a rule left without participants`() {
        val rule = percentRule("hermano" to 100)

        val sanitized = SplitRuleCalc.sanitize(rule, listOf(me))

        assertFalse(sanitized.enabled)
        assertEquals(emptyList<SplitRuleShare>(), sanitized.shares)
    }

    @Test
    fun `sanitize coerces amount and none modes to equal`() {
        val ids = listOf(me, "hermano")
        val shares = listOf(SplitRuleShare(me), SplitRuleShare("hermano"))

        listOf(SplitMode.Amount, SplitMode.None).forEach { mode ->
            val sanitized = SplitRuleCalc.sanitize(
                SplitRule(enabled = true, mode = mode, shares = shares), ids,
            )
            assertEquals(SplitMode.Equal, sanitized.mode)
        }
    }

    @Test
    fun `sanitize falls back to me when the payer no longer exists`() {
        val rule = percentRule(me to 100).copy(payerId = "hermano")

        assertEquals(me, SplitRuleCalc.sanitize(rule, listOf(me)).payerId)
    }

    @Test
    fun `sanitize spreads percents evenly when all of them are zero`() {
        val rule = percentRule(me to 0, "a" to 0, "b" to 0)

        val sanitized = SplitRuleCalc.sanitize(rule, listOf(me, "a", "b"))

        assertEquals(listOf(34, 33, 33), sanitized.shares.map { it.percent })
    }

    @Test
    fun `sanitize rescales percents that do not add up`() {
        val rule = percentRule(me to 15, "hermano" to 75) // suman 90

        val sanitized = SplitRuleCalc.sanitize(rule, listOf(me, "hermano"))

        assertEquals(100, sanitized.shares.sumOf { it.percent })
        assertEquals(listOf(17, 83), sanitized.shares.map { it.percent })
    }

    @Test
    fun `sanitize is idempotent`() {
        val rule = percentRule(me to 15, "hermano" to 75)
        val ids = listOf(me, "hermano")

        val once = SplitRuleCalc.sanitize(rule, ids)

        assertEquals(once, SplitRuleCalc.sanitize(once, ids))
    }

    @Test
    fun `sanitize drops duplicated participants`() {
        val rule = percentRule(me to 50, me to 50)

        assertEquals(1, SplitRuleCalc.sanitize(rule, listOf(me)).shares.size)
    }

    // ---- evenPercents ----

    @Test
    fun `evenPercents adds up to a hundred giving the remainder to the first ids`() {
        assertEquals(listOf(34, 33, 33), SplitRuleCalc.evenPercents(listOf(me, "a", "b")).map { it.percent })
        assertEquals(listOf(50, 50), SplitRuleCalc.evenPercents(listOf(me, "a")).map { it.percent })
        assertEquals(emptyList<SplitRuleShare>(), SplitRuleCalc.evenPercents(emptyList()))
    }

    // ---- implicitDefault ----

    @Test
    fun `implicitDefault includes everyone in a small space`() {
        val rule = SplitRuleCalc.implicitDefault(listOf(me, "hermano"))

        assertEquals(listOf(me, "hermano"), rule.participantIds)
        assertEquals(SplitMode.Equal, rule.mode)
        assertEquals(me, rule.payerId)
        assertFalse(rule.enabled)
    }

    @Test
    fun `implicitDefault includes only me when the space has more than two members`() {
        val rule = SplitRuleCalc.implicitDefault(listOf(me, "a", "b"))

        assertEquals(listOf(me), rule.participantIds)
    }

    // ---- validate ----

    @Test
    fun `validate reports percents that do not add up`() {
        val rule = percentRule(me to 15, "hermano" to 75)

        assertEquals("Suman 90%", SplitRuleCalc.validate(rule, listOf(me, "hermano")))
    }

    @Test
    fun `validate accepts a rule that adds up and an equal one`() {
        assertNull(SplitRuleCalc.validate(percentRule(me to 25, "hermano" to 75), listOf(me, "hermano")))
        assertNull(
            SplitRuleCalc.validate(
                SplitRule(enabled = true, mode = SplitMode.Equal, shares = listOf(SplitRuleShare(me))),
                listOf(me),
            ),
        )
    }

    @Test
    fun `validate reports an empty participant list`() {
        assertEquals(
            "Selecciona al menos una persona",
            SplitRuleCalc.validate(percentRule("hermano" to 100), listOf(me)),
        )
    }
}
