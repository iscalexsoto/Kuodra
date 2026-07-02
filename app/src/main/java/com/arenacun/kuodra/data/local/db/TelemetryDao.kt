package com.arenacun.kuodra.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TelemetryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun enqueue(event: TelemetryEventEntity)

    /** Lote de eventos sin subir, más antiguos primero. */
    @Query("SELECT * FROM telemetry_events WHERE sent = 0 ORDER BY createdAt ASC LIMIT :limit")
    suspend fun pending(limit: Int): List<TelemetryEventEntity>

    @Query("UPDATE telemetry_events SET sent = 1 WHERE id IN (:ids)")
    suspend fun markSent(ids: List<String>)

    /** Elimina lo ya entregado (poda de la cola tras subir). */
    @Query("DELETE FROM telemetry_events WHERE sent = 1")
    suspend fun pruneSent()

    /** Recorta la cola si crece sin control (p. ej. sin red mucho tiempo): deja los más nuevos. */
    @Query(
        "DELETE FROM telemetry_events WHERE sent = 0 AND id NOT IN " +
            "(SELECT id FROM telemetry_events WHERE sent = 0 ORDER BY createdAt DESC LIMIT :keep)",
    )
    suspend fun trimTo(keep: Int)
}
