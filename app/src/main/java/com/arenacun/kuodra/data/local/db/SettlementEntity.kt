package com.arenacun.kuodra.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Corte/liquidación congelado de un espacio de Gastos. Las líneas (saldo por persona) y las
 * transferencias sugeridas se guardan como JSON. Columnas de sync estándar.
 */
@Entity(tableName = "settlements")
data class SettlementEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val space: String,
    val title: String,
    val date: LocalDate,
    val totalCents: Long,
    /** `[{personId, name, net}]` congelado. */
    val linesJson: String = "[]",
    /** `[{fromId, toId, amount}]` sugeridas. */
    val transfersJson: String = "[]",
    val createdAt: Long,
    /** "Corte" | "Payment" (pago individual). */
    val kind: String = "Corte",
    /** Solo pagos: id del corte que lo consumió; "" = vivo. */
    val settledBy: String = "",
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    val remoteUpdated: String = "",
)

@Dao
interface SettlementDao {

    /** Cortes vigentes de un espacio, recientes primero. */
    @Query("SELECT * FROM settlements WHERE owner = :owner AND space = :space AND deleted = 0 ORDER BY createdAt DESC")
    fun observe(owner: String, space: String): Flow<List<SettlementEntity>>

    @Query("SELECT * FROM settlements WHERE id = :id")
    suspend fun find(id: String): SettlementEntity?

    @Query("SELECT * FROM settlements WHERE owner = :owner AND dirty = 1")
    suspend fun dirtyRows(owner: String): List<SettlementEntity>

    @Query("UPDATE settlements SET dirty = 0, remoteUpdated = :remoteUpdated WHERE id = :id")
    suspend fun markSynced(id: String, remoteUpdated: String)

    @Upsert
    suspend fun upsert(settlement: SettlementEntity)

    @Query("UPDATE settlements SET deleted = 1, dirty = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)

    /** Marca los pagos indicados como consumidos por un corte (salen de los balances vivos) + dirty. */
    @Query("UPDATE settlements SET settledBy = :corteId, dirty = 1, updatedAt = :updatedAt WHERE id IN (:ids)")
    suspend fun stampSettledBy(ids: List<String>, corteId: String, updatedAt: Long)
}
