package com.arenacun.kuodra.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `initial preloads a clean editable value marked fresh`() {
        val s = Calc.initial(250.0)
        assertEquals("250", s.expression)
        assertEquals("250", s.display)
        assertTrue(s.fresh)
        assertEquals("2.5", Calc.initial(2.5).expression)
    }

    @Test
    fun `initial of null or zero is a blank state`() {
        assertEquals(CalcState(), Calc.initial(null))
        assertEquals(CalcState(), Calc.initial(0.0))
        assertFalse(Calc.initial(0.0).fresh)
    }

    @Test
    fun `first digit replaces a fresh value, next digit appends`() {
        var s = Calc.initial(250.0)
        s = Calc.press(s, CalcKey.N1)
        assertEquals("1", s.expression)   // reemplaza, no "2501"
        assertFalse(s.fresh)
        s = Calc.press(s, CalcKey.N0)
        assertEquals("10", s.expression)  // ya edita normal
    }

    @Test
    fun `operator continues from a fresh value`() {
        var s = CalcState("100", fresh = true)
        s = Calc.press(s, CalcKey.Plus)
        s = Calc.press(s, CalcKey.N5)
        assertEquals("100+5", s.expression)
        assertEquals(105.0, s.result)
    }

    @Test
    fun `dot starts a new decimal on a fresh value`() {
        val s = Calc.press(CalcState("100", fresh = true), CalcKey.Dot)
        assertEquals("0.", s.expression)
    }

    @Test
    fun `equals leaves the result fresh so the next digit overwrites`() {
        var s = CalcState("2+3*4")
        s = Calc.press(s, CalcKey.Equals)
        assertEquals("14", s.expression)
        assertTrue(s.fresh)
        s = Calc.press(s, CalcKey.N7)
        assertEquals("7", s.expression)
    }

    @Test
    fun `parseAmount reverses formatAmount`() {
        assertEquals(6000.0, Calc.parseAmount("$6,000"))
        assertEquals(119.80, Calc.parseAmount("$119.80"))
        assertEquals(-119.80, Calc.parseAmount("−$119.80"))
        assertNull(Calc.parseAmount("sin monto"))
    }
}
