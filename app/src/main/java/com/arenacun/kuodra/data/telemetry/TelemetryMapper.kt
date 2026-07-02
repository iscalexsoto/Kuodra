package com.arenacun.kuodra.data.telemetry

import com.arenacun.kuodra.data.local.db.TelemetryEventEntity
import com.arenacun.kuodra.data.remote.dto.TelemetryEventDto
import com.arenacun.kuodra.data.remote.dto.TelemetryRecord
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Cola local: el evento completo va serializado en `payload`. */
fun TelemetryRecord.toEntity(json: Json): TelemetryEventEntity =
    TelemetryEventEntity(id = id, createdAt = createdAt, sent = false, payload = json.encodeToString(this))

fun TelemetryEventEntity.toRecord(json: Json): TelemetryRecord =
    json.decodeFromString(payload)

/**
 * Cuerpo para PocketBase. `owner` es la relación al usuario: se usa el del evento y, si es
 * anónimo (pre-login), se atribuye al usuario en sesión al momento de subir ([fallbackOwner]).
 */
fun TelemetryRecord.toDto(fallbackOwner: String): TelemetryEventDto =
    TelemetryEventDto(
        level = level,
        type = type,
        message = message,
        stacktrace = stacktrace,
        breadcrumbs = breadcrumbs,
        context = context,
        tags = tags,
        release = release,
        session_id = sessionId,
        fingerprint = fingerprint,
        client_created = createdAt,
        owner = userId.ifEmpty { fallbackOwner },
    )
