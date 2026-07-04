package com.arenacun.kuodra.domain.scan

/**
 * Eslabón de la cadena de parseo de tickets (Template → Mistral → Regex). Recibe el texto OCR ya
 * pasado por [OcrNormalizer] y devuelve `null` si este parser no puede/no debe resolver (sin red,
 * error del servidor, sin template registrado…), para que la cadena pase al siguiente eslabón.
 * Un parser **nunca lanza**: sus fallos internos se traducen a `null`.
 */
interface TicketParser {
    val source: TicketParseSource
    suspend fun parse(normalizedText: String): ParsedTicket?
}
