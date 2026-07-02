package com.arenacun.kuodra.data.remote.dto

import kotlinx.serialization.Serializable

/** Una miga de la traza (breadcrumb). `level` es el nombre de `LogLevel`. */
@Serializable
data class BreadcrumbDto(
    val timestamp: Long,
    val category: String,
    val message: String,
    val level: String = "Info",
    val data: Map<String, String> = emptyMap(),
)

/**
 * Forma canónica y **serializable** de un evento de telemetría. Se usa en tres sitios:
 * la cola local (columna `payload` de Room), el *spool* de crash en disco, y como fuente para
 * construir el [TelemetryEventDto] que se sube a PocketBase.
 */
@Serializable
data class TelemetryRecord(
    val id: String,
    val createdAt: Long,
    val level: String,
    val type: String,          // log | error | crash
    val message: String,
    val stacktrace: String = "",
    val breadcrumbs: List<BreadcrumbDto> = emptyList(),
    val context: Map<String, String> = emptyMap(),
    val tags: Map<String, String> = emptyMap(),
    val userId: String = "",
    val userEmail: String = "",
    val release: String = "",
    val sessionId: String = "",
    val fingerprint: String = "",
)

/**
 * Cuerpo del `POST` a la colección `telemetry_events` de PocketBase. Nombres en snake_case para
 * casar con los campos de la colección; `owner` es la relación al usuario (id).
 */
@Serializable
data class TelemetryEventDto(
    val level: String,
    val type: String,
    val message: String,
    val stacktrace: String,
    val breadcrumbs: List<BreadcrumbDto>,
    val context: Map<String, String>,
    val tags: Map<String, String>,
    val release: String,
    val session_id: String,
    val fingerprint: String,
    val client_created: Long,
    val owner: String,
)
