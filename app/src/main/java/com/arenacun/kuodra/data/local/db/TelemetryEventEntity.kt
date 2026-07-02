package com.arenacun.kuodra.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Fila de la cola de telemetría pendiente de subir. El evento completo va serializado en [payload]
 * (un `TelemetryRecord` en JSON); las columnas sueltas son solo para ordenar y filtrar sin
 * deserializar. `sent = true` marca lo ya entregado (se poda después).
 */
@Entity(tableName = "telemetry_events")
data class TelemetryEventEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val sent: Boolean = false,
    val payload: String,
)
