package com.arenacun.kuodra.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Fila persistida de un movimiento. Modelo de almacenamiento: monto en centavos y categoría por
 * referencia. Incluye las **columnas de sincronización** desde el día 1 (aunque el sync llega en
 * Fase 2) para no migrar el esquema después:
 * - [owner] id del usuario dueño (aislamiento multi-cuenta en el mismo dispositivo).
 * - [updatedAt] epoch millis del último cambio conocido (cursor de deltas + last-write-wins).
 * - [deleted] tombstone: borrado lógico para propagar la baja al sincronizar.
 * - [dirty] hay cambios locales sin subir.
 */
@Entity(tableName = "movements")
data class MovementEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val amountCents: Long,
    val categoryId: String,
    val title: String,
    val note: String,
    val date: LocalDate,
    val payer: String?,
    val splitNames: List<String>,
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    /** `updated` del servidor de la última sincronización (vacío = nunca subido). */
    val remoteUpdated: String = "",
)
