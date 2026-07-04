package com.arenacun.kuodra.domain.scan

import com.arenacun.kuodra.domain.model.Money
import java.time.LocalDate

/** Origen de la imagen escaneada. Se persiste en el [com.arenacun.kuodra.domain.model.Movement]. */
enum class ScanSource { Camera, Gallery }

/**
 * Quién produjo el parseo. El orden refleja la cadena de prioridad: `Template` (futuro, tickets
 * de comercios conocidos sin red) → `Mistral` (remoto) → `Regex` (fallback local).
 */
enum class TicketParseSource { Template, Mistral, Regex }

/** Partida detectada en el ticket (producto + precio). */
data class ParsedTicketItem(
    val concept: String,
    val amount: Money,
)

/**
 * Resultado del análisis de un ticket. Todos los campos son *best-effort*: null / lista vacía
 * significa "no detectado". El formulario de alta se puebla con lo que haya y el usuario edita.
 */
data class ParsedTicket(
    val merchant: String? = null,
    val total: Money? = null,
    val date: LocalDate? = null,
    val items: List<ParsedTicketItem> = emptyList(),
    val source: TicketParseSource,
) {
    val isEmpty: Boolean
        get() = merchant == null && total == null && date == null && items.isEmpty()
}

/**
 * Resultado completo de un escaneo: el raw OCR **sin normalizar** (se persiste como material para
 * los templates futuros), el parseo y de dónde salió la imagen.
 */
data class TicketScan(
    val rawText: String,
    val parsed: ParsedTicket,
    val scanSource: ScanSource,
)
