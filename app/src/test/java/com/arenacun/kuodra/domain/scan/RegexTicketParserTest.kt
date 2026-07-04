package com.arenacun.kuodra.domain.scan

import com.arenacun.kuodra.domain.model.Money
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RegexTicketParserTest {

    private val today = LocalDate.of(2026, 7, 2)
    private val parser = RegexTicketParser(today = { today })

    private suspend fun parse(raw: String): ParsedTicket = parser.parse(OcrNormalizer.normalize(raw))

    @Test
    fun `parses a convenience store ticket`() = runTest {
        val ticket = parse(
            """
            OXXO
            RFC OXX970814HS9
            SUC 50123 GUADALAJARA
            30/06/2026 21:43
            COCA COLA 600ML  19.00
            SABRITAS ORIGINAL 52G  18.50
            GANSITO  22.00
            SUBTOTAL  59.50
            TOTAL  59.50
            EFECTIVO  100.00
            CAMBIO  40.50
            """.trimIndent(),
        )
        assertEquals("OXXO", ticket.merchant)
        assertEquals(Money(5950), ticket.total)
        assertEquals(LocalDate.of(2026, 6, 30), ticket.date)
        assertEquals(
            listOf(
                ParsedTicketItem("COCA COLA 600ML", Money(1900)),
                ParsedTicketItem("SABRITAS ORIGINAL 52G", Money(1850)),
                ParsedTicketItem("GANSITO", Money(2200)),
            ),
            ticket.items,
        )
        assertEquals(TicketParseSource.Regex, ticket.source)
    }

    @Test
    fun `parses thousands separators and label on its own line`() = runTest {
        val ticket = parse(
            """
            BODEGA MAYORISTA DEL CENTRO
            PANTALLA 55 PULGADAS  8,999.00
            SOPORTE PARED  1,250.50
            TOTAL A PAGAR
            $10,249.50
            """.trimIndent(),
        )
        assertEquals(Money(1_024_950), ticket.total)
        assertEquals(Money(899_900), ticket.items[0].amount)
        assertEquals(Money(125_050), ticket.items[1].amount)
    }

    @Test
    fun `restaurant ticket ignores tip and payment lines as items`() = runTest {
        val ticket = parse(
            """
            LA CASA DE TONO
            2 de julio de 2026
            POZOLE GRANDE  95.00
            AGUA JAMAICA  35.00
            PROPINA  15.00
            TOTAL 145.00
            TARJETA  145.00
            """.trimIndent(),
        )
        assertEquals("LA CASA DE TONO", ticket.merchant)
        assertEquals(Money(14_500), ticket.total)
        assertEquals(LocalDate.of(2026, 7, 2), ticket.date)
        assertEquals(listOf("POZOLE GRANDE", "AGUA JAMAICA"), ticket.items.map { it.concept })
    }

    @Test
    fun `without total label falls back to the biggest amount`() = runTest {
        val ticket = parse(
            """
            ABARROTES DONA MARY
            LECHE  28.00
            PAN  42.50
            70.50
            """.trimIndent(),
        )
        assertEquals(Money(7050), ticket.total)
    }

    @Test
    fun `degraded ticket yields a partial result`() = runTest {
        val ticket = parse(
            """
            #%&@!!
            123456789
            88.00
            """.trimIndent(),
        )
        assertNull(ticket.merchant)
        assertNull(ticket.date)
        assertEquals(Money(8800), ticket.total)
        assertTrue(ticket.items.isEmpty())
    }

    @Test
    fun `empty text yields an empty result`() = runTest {
        val ticket = parse("")
        assertTrue(ticket.isEmpty)
        assertEquals(TicketParseSource.Regex, ticket.source)
    }

    @Test
    fun `future dates are discarded`() = runTest {
        val ticket = parse(
            """
            TIENDA
            25/12/2026
            TOTAL 10.00
            """.trimIndent(),
        )
        assertNull(ticket.date)
    }

    @Test
    fun `parses iso and two digit year dates`() = runTest {
        assertEquals(LocalDate.of(2026, 6, 15), parse("FECHA 2026-06-15\nTOTAL 5.00").date)
        assertEquals(LocalDate.of(2026, 1, 9), parse("09/01/26\nTOTAL 5.00").date)
    }

    @Test
    fun `subtotal is not the total`() = runTest {
        val ticket = parse(
            """
            SUPER X
            SUBTOTAL  100.00
            TOTAL  116.00
            """.trimIndent(),
        )
        assertEquals(Money(11_600), ticket.total)
    }

    @Test
    fun `merchant skips fiscal noise lines`() = runTest {
        val ticket = parse(
            """
            RFC ABC123456XYZ
            TICKET 00012
            FARMACIA GUADALAJARA
            PARACETAMOL  35.00
            """.trimIndent(),
        )
        assertEquals("FARMACIA GUADALAJARA", ticket.merchant)
    }
}
