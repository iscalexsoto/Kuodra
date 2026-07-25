package com.arenacun.kuodra.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Espacio de Gastos compartidos (un grupo: roomies, viaje, etc.). Incluye las columnas de sync
 * estándar ([owner]/[updatedAt]/[deleted]/[dirty]/[remoteUpdated]). [archived] permite guardar el
 * espacio sin borrarlo (p. ej. al terminar un viaje).
 */
@Entity(tableName = "spaces")
data class SpaceEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val name: String,
    val archived: Boolean = false,
    val reminderEnabled: Boolean = true,
    /** Regla de división por defecto serializada como JSON (vacío = `SplitRule.Default`). */
    val splitRuleJson: String = "",
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    val remoteUpdated: String = "",
)

@Dao
interface SpaceDao {

    /** Espacios vigentes del usuario (incluye archivados; la UI los separa), por nombre. */
    @Query("SELECT * FROM spaces WHERE owner = :owner AND deleted = 0 ORDER BY name")
    fun observe(owner: String): Flow<List<SpaceEntity>>

    @Query("SELECT * FROM spaces WHERE id = :id")
    suspend fun find(id: String): SpaceEntity?

    @Query("SELECT * FROM spaces WHERE owner = :owner AND dirty = 1")
    suspend fun dirtyRows(owner: String): List<SpaceEntity>

    @Query("UPDATE spaces SET dirty = 0, remoteUpdated = :remoteUpdated WHERE id = :id")
    suspend fun markSynced(id: String, remoteUpdated: String)

    @Upsert
    suspend fun upsert(space: SpaceEntity)

    @Query("UPDATE spaces SET deleted = 1, dirty = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)
}
