package com.arenacun.kuodra.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class CountryCodeTest {

    @Test
    fun `split separates the mexican dial code from the local number`() {
        assertEquals("+52" to "5512345678", Countries.split("+525512345678"))
    }

    @Test
    fun `split prefers the longest matching dial code`() {
        // +503 (El Salvador) no debe confundirse con +5.
        assertEquals("+503" to "71234567", Countries.split("+50371234567"))
    }

    @Test
    fun `split handles the one-digit code`() {
        assertEquals("+1" to "2025550123", Countries.split("+12025550123"))
    }

    @Test
    fun `empty phone falls back to the default country`() {
        assertEquals(Countries.DEFAULT.dialCode to "", Countries.split(""))
    }

    @Test
    fun `unknown code keeps the digits as local under the default`() {
        val (dial, local) = Countries.split("+99912345")
        assertEquals(Countries.DEFAULT.dialCode, dial)
        assertEquals("99912345", local)
    }
}
