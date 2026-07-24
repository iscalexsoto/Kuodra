package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppMessageTest {

    @Test
    fun `debtor message asks for payment and includes name space and amount`() {
        val message = WhatsAppMessage.build("Andrea", Money(-45000), "Casa Roma")
        assertTrue(message.contains("Andrea"))
        assertTrue(message.contains("Casa Roma"))
        assertTrue(message.contains("$450"))
        assertTrue(message.contains("pasar"))
    }

    @Test
    fun `creditor message acknowledges owing them`() {
        val message = WhatsAppMessage.build("Beto", Money(20000), "Casa Roma")
        assertTrue(message.contains("te debo"))
        assertTrue(message.contains("$200"))
    }

    @Test
    fun `settled message says even`() {
        val message = WhatsAppMessage.build("Caro", Money.Zero, "Casa Roma")
        assertTrue(message.contains("a mano"))
    }
}
