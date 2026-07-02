package com.arenacun.kuodra.data.telemetry

import com.arenacun.kuodra.data.remote.dto.TelemetryRecord
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * *Spool* en disco para crashes fatales. En el manejador de excepciones no capturadas no podemos
 * confiar en Room ni en corrutinas (el proceso está muriendo), así que el evento se escribe como un
 * archivo JSON **síncrono**. En el siguiente arranque, [drain] los mueve a la cola de Room.
 *
 * Todas las operaciones son best-effort (nunca lanzan): perder un crash es preferible a crashear
 * dentro del manejador de crashes.
 */
class CrashSpool(
    private val dir: File,
    private val json: Json,
) {

    /** Escritura síncrona a prueba de muerte del proceso. No lanza. */
    fun writeBlocking(record: TelemetryRecord) {
        runCatching {
            if (!dir.exists()) dir.mkdirs()
            File(dir, "${record.id}.json").writeText(json.encodeToString(record))
        }
    }

    /** Lee y elimina todos los crashes en *spool*. No lanza; ignora archivos corruptos. */
    fun drain(): List<TelemetryRecord> {
        val files = runCatching { dir.listFiles { f -> f.extension == "json" } }.getOrNull() ?: return emptyList()
        return files.mapNotNull { f ->
            val record = runCatching { json.decodeFromString<TelemetryRecord>(f.readText()) }.getOrNull()
            runCatching { f.delete() }
            record
        }
    }
}
