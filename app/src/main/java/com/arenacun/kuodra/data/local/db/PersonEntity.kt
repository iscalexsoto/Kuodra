package com.arenacun.kuodra.data.local.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Contacto de un espacio de Gastos: registro local (Nombre + Teléfono), sin cuenta ni conexión. El
 * [space] lo ata a un espacio concreto. Columnas de sync estándar.
 */
@Entity(tableName = "persons")
data class PersonEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val space: String,
    val name: String,
    val phone: String = "",
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    val remoteUpdated: String = "",
)

@Dao
interface PersonDao {

    /** Contactos vigentes de un espacio, por nombre. */
    @Query("SELECT * FROM persons WHERE owner = :owner AND space = :space AND deleted = 0 ORDER BY name")
    fun observe(owner: String, space: String): Flow<List<PersonEntity>>

    @Query("SELECT * FROM persons WHERE id = :id")
    suspend fun find(id: String): PersonEntity?

    @Query("SELECT * FROM persons WHERE owner = :owner AND dirty = 1")
    suspend fun dirtyRows(owner: String): List<PersonEntity>

    @Query("UPDATE persons SET dirty = 0, remoteUpdated = :remoteUpdated WHERE id = :id")
    suspend fun markSynced(id: String, remoteUpdated: String)

    @Upsert
    suspend fun upsert(person: PersonEntity)

    @Query("UPDATE persons SET deleted = 1, dirty = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)
}
