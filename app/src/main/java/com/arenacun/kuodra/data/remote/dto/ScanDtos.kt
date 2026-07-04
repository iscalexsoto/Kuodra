package com.arenacun.kuodra.data.remote.dto

import kotlinx.serialization.Serializable

/** Petición al proxy de análisis de tickets (`POST /api/kuodra/analyze-ticket`). */
@Serializable
data class TicketAnalysisRequest(val text: String)

/** Partida detectada por el análisis. `amount` en unidades mayores (pesos). */
@Serializable
data class AnalyzedItemDto(
    val concept: String = "",
    val amount: Double = 0.0,
)

/**
 * Respuesta **versionada** del proxy (el servidor arma este shape a partir del JSON de Mistral).
 * Todos los campos de datos son opcionales ⇒ la v2 podrá añadir p. ej. `template` sin romper
 * clientes v1 (`ignoreUnknownKeys` en el cliente).
 */
@Serializable
data class TicketAnalysisDto(
    val version: Int = 1,
    val merchant: String? = null,
    /** Total en unidades mayores (ej. 187.50); el cliente lo convierte con `Money.ofMajor`. */
    val total: Double? = null,
    /** Fecha ISO `yyyy-MM-dd` o null si no fue legible. */
    val date: String? = null,
    val items: List<AnalyzedItemDto> = emptyList(),
)
