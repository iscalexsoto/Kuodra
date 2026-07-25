package com.arenacun.kuodra.data.scan

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.mapper.toParsedTicket
import com.arenacun.kuodra.data.remote.TicketAnalysisApi
import com.arenacun.kuodra.domain.scan.ParsedTicket
import com.arenacun.kuodra.domain.scan.TicketParseSource
import com.arenacun.kuodra.domain.scan.TicketParser
import com.arenacun.kuodra.domain.telemetry.Telemetry
import kotlinx.coroutines.CancellationException

/**
 * Eslabón remoto de la cadena de parseo: manda el OCR normalizado al proxy Mistral de PocketBase.
 * Cualquier impedimento (sin sesión, sin red, timeout, 5xx, JSON inválido) devuelve `null` para
 * que la cadena caiga al parser regex local; el usuario solo nota menor calidad de parseo.
 */
class MistralTicketParser(
    private val api: TicketAnalysisApi,
    private val sessionStore: SessionStore,
    private val telemetry: Telemetry,
) : TicketParser {

    override val source: TicketParseSource = TicketParseSource.Mistral

    override suspend fun parse(normalizedText: String): ParsedTicket? {
        val token = sessionStore.token() ?: return null
        return runCatching { api.analyze(normalizedText, token).toParsedTicket() }
            .onFailure {
                // La cancelación no es un fallo del parseo: se propaga en vez de degradar a regex.
                if (it is CancellationException) throw it
                telemetry.breadcrumb("scan", "mistral parse failed", mapOf("error" to it.message.orEmpty()))
            }
            .getOrNull()
    }
}
