package com.arenacun.kuodra.data.mapper

import com.arenacun.kuodra.data.remote.dto.TicketAnalysisDto
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.scan.ParsedTicket
import com.arenacun.kuodra.domain.scan.ParsedTicketItem
import com.arenacun.kuodra.domain.scan.TicketParseSource
import java.time.LocalDate

/**
 * Respuesta del proxy de análisis → dominio. Defensivo con datos del modelo: fecha ilegible ⇒
 * null, partidas sin concepto o con monto ≤ 0 se descartan.
 */
fun TicketAnalysisDto.toParsedTicket(): ParsedTicket = ParsedTicket(
    merchant = merchant?.trim()?.ifEmpty { null },
    total = total?.takeIf { it > 0 }?.let { Money.ofMajor(it) },
    date = date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    items = items.mapNotNull { item ->
        val concept = item.concept.trim()
        if (concept.isEmpty() || item.amount <= 0) null
        else ParsedTicketItem(concept, Money.ofMajor(item.amount))
    },
    source = TicketParseSource.Mistral,
)
