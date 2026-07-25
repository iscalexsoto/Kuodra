package com.arenacun.kuodra.data.scan

import com.arenacun.kuodra.TestPrefsRule
import com.arenacun.kuodra.data.remote.TicketAnalysisApi
import com.arenacun.kuodra.data.remote.dto.AnalyzedItemDto
import com.arenacun.kuodra.data.remote.dto.TicketAnalysisDto
import com.arenacun.kuodra.domain.scan.TicketParseSource
import com.arenacun.kuodra.domain.telemetry.NoOpTelemetry
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MistralTicketParserTest {

    @get:Rule
    val prefs = TestPrefsRule()

    private fun sessionStore() = prefs.sessionStore("scan.preferences_pb")

    private class FakeTicketAnalysisApi(
        var response: TicketAnalysisDto = TicketAnalysisDto(),
        var throws: Throwable? = null,
    ) : TicketAnalysisApi {
        var lastText: String? = null
        var lastToken: String? = null
        override suspend fun analyze(text: String, token: String): TicketAnalysisDto {
            lastText = text
            lastToken = token
            throws?.let { throw it }
            return response
        }
    }

    @Test
    fun `maps a successful analysis to a Mistral parsed ticket`() = runTest {
        val api = FakeTicketAnalysisApi(
            TicketAnalysisDto(
                merchant = "OXXO",
                total = 59.50,
                date = "2026-06-30",
                items = listOf(
                    AnalyzedItemDto("Coca 600ml", 19.0),
                    AnalyzedItemDto("", 5.0),        // sin concepto ⇒ fuera
                    AnalyzedItemDto("Gansito", 0.0), // monto 0 ⇒ fuera
                ),
            ),
        )
        val session = sessionStore().apply { save("tok_1", "u1", "u1@x.com", "U") }
        val parser = MistralTicketParser(api, session, NoOpTelemetry)

        val ticket = parser.parse("OXXO\nTOTAL 59.50")!!

        assertEquals(TicketParseSource.Mistral, ticket.source)
        assertEquals("OXXO", ticket.merchant)
        assertEquals(5950L, ticket.total!!.cents)
        assertEquals(LocalDate.of(2026, 6, 30), ticket.date)
        assertEquals(listOf("Coca 600ml"), ticket.items.map { it.concept })
        assertEquals("tok_1", api.lastToken)
        assertEquals("OXXO\nTOTAL 59.50", api.lastText)
    }

    @Test
    fun `without a session it yields null and never calls the api`() = runTest {
        val api = FakeTicketAnalysisApi()
        val parser = MistralTicketParser(api, sessionStore(), NoOpTelemetry)

        assertNull(parser.parse("TEXTO"))
        assertNull(api.lastText)
    }

    @Test
    fun `an api failure yields null so the chain falls back to regex`() = runTest {
        val api = FakeTicketAnalysisApi(throws = RuntimeException("502"))
        val session = sessionStore().apply { save("tok_1", "u1", "u1@x.com", "U") }
        val parser = MistralTicketParser(api, session, NoOpTelemetry)

        assertNull(parser.parse("TEXTO"))
    }

    /**
     * `runBlocking` en vez de `runTest`: la aserción es que `parse` LANZA la cancelación, y dejar que
     * una `CancellationException` escape del cuerpo de un `runTest` se confunde con que el test mismo
     * se canceló.
     */
    @Test
    fun `a cancellation propagates instead of degrading to the next parser`() {
        val api = FakeTicketAnalysisApi(throws = CancellationException("el usuario salió"))
        val parser = runBlocking {
            val session = sessionStore().apply { save("tok_1", "u1", "u1@x.com", "U") }
            MistralTicketParser(api, session, NoOpTelemetry)
        }

        // Cancelarse no es "no pude parsear": tragarlo rompería la cancelación cooperativa.
        assertThrows(CancellationException::class.java) {
            runBlocking { parser.parse("TEXTO") }
        }
    }

    @Test
    fun `an invalid date maps to null date without dropping the rest`() = runTest {
        val api = FakeTicketAnalysisApi(TicketAnalysisDto(merchant = "Super", total = 10.0, date = "30/06/2026"))
        val session = sessionStore().apply { save("tok_1", "u1", "u1@x.com", "U") }
        val parser = MistralTicketParser(api, session, NoOpTelemetry)

        val ticket = parser.parse("X")!!

        assertNull(ticket.date)
        assertEquals("Super", ticket.merchant)
        assertEquals(1000L, ticket.total!!.cents)
        assertTrue(ticket.items.isEmpty())
    }
}
