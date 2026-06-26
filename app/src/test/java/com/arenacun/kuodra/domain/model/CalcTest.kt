package com.arenacun.kuodra.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcTest {

    @Test
    fun `evaluates a single number`() {
        assertEquals(340.0, Calc.evaluate("340"))
    }

    @Test
    fun `respects operator precedence`() {
        assertEquals(14.0, Calc.evaluate("2+3*4"))
        assertEquals(2.5, Calc.evaluate("10/4"))
        assertEquals(7.0, Calc.evaluate("1+2*3"))
    }

    @Test
    fun `division by zero is null`() {
        assertNull(Calc.evaluate("5/0"))
    }

    @Test
    fun `trailing operator is incomplete`() {
        assertNull(Calc.evaluate("5+"))
        assertNull(Calc.evaluate(""))
    }

    @Test
    fun `formats whole amounts with thousands separator`() {
        assertEquals("$1,240", Calc.formatAmount(1240.0))
        assertEquals("$8,000", Calc.formatAmount(8000.0))
        assertEquals("$340", Calc.formatAmount(340.0))
    }

    @Test
    fun `formats fractional amounts with two decimals`() {
        assertEquals("$119.80", Calc.formatAmount(119.8))
        assertEquals("$248", Calc.formatAmount(248.0))
    }

    @Test
    fun `digit and operator presses build an expression`() {
        var s = CalcState()
        listOf(CalcKey.N3, CalcKey.N4, CalcKey.N0, CalcKey.Plus, CalcKey.N6, CalcKey.N0)
            .forEach { s = Calc.press(s, it) }
        assertEquals("340+60", s.expression)
        assertEquals(400.0, s.result)
    }

    @Test
    fun `equals collapses the expression to its result`() {
        var s = CalcState("2+3*4")
        s = Calc.press(s, CalcKey.Equals)
        assertEquals("14", s.expression)
    }

    @Test
    fun `replacing a trailing operator keeps the expression valid`() {
        var s = CalcState("5")
        s = Calc.press(s, CalcKey.Plus)
        s = Calc.press(s, CalcKey.Minus) // reemplaza + por -
        s = Calc.press(s, CalcKey.N2)
        assertEquals("5-2", s.expression)
        assertEquals(3.0, s.result)
    }

    @Test
    fun `only one dot per number`() {
        var s = CalcState("1")
        s = Calc.press(s, CalcKey.Dot)
        s = Calc.press(s, CalcKey.N5)
        s = Calc.press(s, CalcKey.Dot) // ignorado
        assertEquals("1.5", s.expression)
    }
}
