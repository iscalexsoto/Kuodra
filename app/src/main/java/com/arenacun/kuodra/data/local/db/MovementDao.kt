package com.arenacun.kuodra.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MovementDao {

    /** Movimientos vigentes del usuario (excluye tombstones), recientes primero. */
    @Query("SELECT * FROM movements WHERE owner = :owner AND deleted = 0 ORDER BY date DESC")
    fun observe(owner: String): Flow<List<MovementEntity>>

    /** Movimientos vigentes de un espacio concreto (`""` = Personal), recientes primero. */
    @Query("SELECT * FROM movements WHERE owner = :owner AND space = :space AND deleted = 0 ORDER BY date DESC")
    fun observe(owner: String, space: String): Flow<List<MovementEntity>>

    @Query("SELECT * FROM movements WHERE id = :id")
    suspend fun find(id: String): MovementEntity?

    /** Filas con cambios locales sin subir (para el push del sync). */
    @Query("SELECT * FROM movements WHERE owner = :owner AND dirty = 1")
    suspend fun dirtyRows(owner: String): List<MovementEntity>

    /** Marca una fila como sincronizada tras subirla: limpia dirty y guarda el `updated` remoto. */
    @Query("UPDATE movements SET dirty = 0, remoteUpdated = :remoteUpdated WHERE id = :id")
    suspend fun markSynced(id: String, remoteUpdated: String)

    @Upsert
    suspend fun upsert(movement: MovementEntity)

    /** Borrado lógico: marca tombstone + dirty para que la baja se propague al sincronizar. */
    @Query("UPDATE movements SET deleted = 1, dirty = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    /** Estampa un corte sobre los gastos indicados (los saca de los balances vivos) + dirty. */
    @Query("UPDATE movements SET settlementId = :settlementId, dirty = 1, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun stampSettlement(ids: List<String>, settlementId: String, updatedAt: Long)
}
