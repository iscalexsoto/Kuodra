package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.scan.OcrEngine
import com.arenacun.kuodra.domain.scan.ParsedTicket
import com.arenacun.kuodra.domain.scan.RegexTicketParser
import com.arenacun.kuodra.domain.scan.ScanSource
import com.arenacun.kuodra.domain.scan.TicketParseSource
import com.arenacun.kuodra.domain.scan.TicketParser
import com.arenacun.kuodra.domain.telemetry.LogLevel
import com.arenacun.kuodra.domain.telemetry.Telemetry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScanTicketUseCaseTest {

    private class FakeOcrEngine(
        private val result: Result<String> = Result.success("TIENDA\nTOTAL 10.00"),
    ) : OcrEngine {
        var lastUri: String? = null
        override suspend fun recognize(imageUri: String): Result<String> {
            lastUri = imageUri
            return result
        }
    }

    /** Parser configurable: devuelve [ticket], lanza, o registra que fue llamado. */
    private class FakeParser(
        override val source: TicketParseSource,
        private val ticket: ParsedTicket? = null,
        private val throws: Boolean = false,
    ) : TicketParser {
        var called = false
        var lastText: String? = null
        override suspend fun parse(normalizedText: String): ParsedTicket? {
            called = true
            lastText = normalizedText
            if (throws) error("parser roto")
            return ticket
        }
    }

    private class FakeTelemetry : Telemetry {
        val breadcrumbs = mutableListOf<String>()
        val captured = mutableListOf<Throwable>()
        override fun breadcrumb(category: String, message: String, data: Map<String, String>) {
            breadcrumbs += "$category: $message"
        }
        override fun log(level: LogLevel, message: String, tags: Map<String, String>, throwable: Throwable?) = Unit
        override fun capture(throwable: Throwable, level: LogLevel, context: Map<String, String>) {
            captured += throwable
        }
        override fun captureFatalBlocking(throwable: Throwable) = Unit
        override fun setUser(id: String?, email: String?) = Unit
        override fun flush() = Unit
    }

    private fun mistralTicket() = ParsedTicket(
        merchant = "OXXO",
        total = Money(1000),
        source = TicketParseSource.Mistral,
    )

    @Test
    fun `first parser that resolves wins and the rest are not called`() = runTest {
        val mistral = FakeParser(TicketParseSource.Mistral, ticket = mistralTicket())
        val regex = FakeParser(TicketParseSource.Regex, ticket = ParsedTicket(source = TicketParseSource.Regex))
        val useCase = ScanTicketUseCase(FakeOcrEngine(), listOf(mistral, regex), FakeTelemetry())

        val scan = useCase("file://x.jpg", ScanSource.Camera).getOrThrow()

        assertEquals(TicketParseSource.Mistral, scan.parsed.source)
        assertTrue(mistral.called)
        assertTrue(!regex.called)
        assertEquals(ScanSource.Camera, scan.scanSource)
    }

    @Test
    fun `null from the first parser falls through to the next`() = runTest {
        val mistral = FakeParser(TicketParseSource.Mistral, ticket = null)
        val regex = FakeParser(TicketParseSource.Regex, ticket = ParsedTicket(source = TicketParseSource.Regex))
        val useCase = ScanTicketUseCase(FakeOcrEngine(), listOf(mistral, regex), FakeTelemetry())

        val scan = useCase("file://x.jpg", ScanSource.Gallery).getOrThrow()

        assertEquals(TicketParseSource.Regex, scan.parsed.source)
        assertTrue(mistral.called)
        assertTrue(regex.called)
    }

    @Test
    fun `a throwing parser does not break the chain`() = runTest {
        val broken = FakeParser(TicketParseSource.Mistral, throws = true)
        val regex = FakeParser(TicketParseSource.Regex, ticket = ParsedTicket(source = TicketParseSource.Regex))
        val useCase = ScanTicketUseCase(FakeOcrEngine(), listOf(broken, regex), FakeTelemetry())

        val scan = useCase("file://x.jpg", ScanSource.Camera).getOrThrow()

        assertEquals(TicketParseSource.Regex, scan.parsed.source)
    }

    @Test
    fun `ocr failure fails the scan and is captured in telemetry`() = runTest {
        val boom = RuntimeException("sin texto")
        val telemetry = FakeTelemetry()
        val parser = FakeParser(TicketParseSource.Regex, ticket = ParsedTicket(source = TicketParseSource.Regex))
        val useCase = ScanTicketUseCase(FakeOcrEngine(Result.failure(boom)), listOf(parser), telemetry)

        val result = useCase("file://x.jpg", ScanSource.Camera)

        assertTrue(result.isFailure)
        assertEquals(boom, result.exceptionOrNull())
        assertEquals(listOf<Throwable>(boom), telemetry.captured)
        assertTrue(!parser.called)
    }

    @Test
    fun `parsers receive the normalized text but rawText stays untouched`() = runTest {
        val raw = "  OXXO   \n\n\nTOTAL $ 59 . 50  "
        val parser = FakeParser(TicketParseSource.Regex, ticket = ParsedTicket(source = TicketParseSource.Regex))
        val useCase = ScanTicketUseCase(FakeOcrEngine(Result.success(raw)), listOf(parser), FakeTelemetry())

        val scan = useCase("file://x.jpg", ScanSource.Camera).getOrThrow()

        assertEquals("OXXO\n\nTOTAL $59.50", parser.lastText)
        assertEquals(raw, scan.rawText)
    }

    @Test
    fun `if every parser gives up the result is an empty regex ticket`() = runTest {
        val mistral = FakeParser(TicketParseSource.Mistral, ticket = null)
        val useCase = ScanTicketUseCase(FakeOcrEngine(), listOf(mistral), FakeTelemetry())

        val scan = useCase("file://x.jpg", ScanSource.Camera).getOrThrow()

        assertTrue(scan.parsed.isEmpty)
        assertEquals(TicketParseSource.Regex, scan.parsed.source)
    }

    @Test
    fun `emits a breadcrumb with the winning parser`() = runTest {
        val telemetry = FakeTelemetry()
        val useCase = ScanTicketUseCase(FakeOcrEngine(), listOf(RegexTicketParser()), telemetry)

        useCase("file://x.jpg", ScanSource.Camera).getOrThrow()

        assertEquals(listOf("scan: ticket parsed via Regex"), telemetry.breadcrumbs)
    }

    @Test
    fun `end to end with the real regex parser`() = runTest {
        val ocr = FakeOcrEngine(Result.success("OXXO\nCOCA 600ML  19.00\nTOTAL  19.00"))
        val useCase = ScanTicketUseCase(ocr, listOf(RegexTicketParser()), FakeTelemetry())

        val scan = useCase("content://media/1", ScanSource.Gallery).getOrThrow()

        assertEquals("OXXO", scan.parsed.merchant)
        assertEquals(Money(1900), scan.parsed.total)
        assertNull(scan.parsed.date)
        assertEquals("content://media/1", ocr.lastUri)
    }
}
