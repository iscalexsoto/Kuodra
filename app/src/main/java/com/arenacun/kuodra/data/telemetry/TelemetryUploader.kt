package com.arenacun.kuodra.data.telemetry

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.TelemetryDao
import com.arenacun.kuodra.data.remote.TelemetryApi
import kotlinx.serialization.json.Json

/**
 * Motor de entrega (Kotlin puro, testeable). El worker solo lo dispara.
 *
 * 1. Vuelca los crashes del *spool* de disco a la cola de Room (durable).
 * 2. Si **no hay sesión**, termina con éxito dejando todo en cola (se subirá tras el login) — igual
 *    criterio que [com.arenacun.kuodra.data.sync.SyncManager].
 * 3. Con sesión, sube por lotes, marca lo enviado y poda.
 */
class TelemetryUploader(
    private val dao: TelemetryDao,
    private val api: TelemetryApi,
    private val sessionStore: SessionStore,
    private val spool: CrashSpool,
    private val json: Json,
) {

    suspend fun upload(): Result<Unit> = runCatching {
        // 1 · crashes persistidos en disco → cola durable
        spool.drain().forEach { dao.enqueue(it.toEntity(json)) }
        dao.trimTo(MAX_QUEUE)

        // 2 · sin sesión: se conserva la cola
        val token = sessionStore.token() ?: return@runCatching
        val owner = sessionStore.userId().orEmpty()

        // 3 · subida por lotes
        while (true) {
            val batch = dao.pending(BATCH)
            if (batch.isEmpty()) break
            batch.forEach { entity ->
                api.send(entity.toRecord(json).toDto(owner), token)
            }
            dao.markSent(batch.map { it.id })
        }
        dao.pruneSent()
    }

    private companion object {
        const val BATCH = 50
        const val MAX_QUEUE = 500
    }
}
