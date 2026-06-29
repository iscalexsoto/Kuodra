package com.arenacun.kuodra.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    /** Catálogo vigente del usuario (excluye archivadas y tombstones). */
    @Query("SELECT * FROM categories WHERE owner = :owner AND deleted = 0 AND archived = 0")
    fun observe(owner: String): Flow<List<CategoryEntity>>

    @Query("SELECT COUNT(*) FROM categories WHERE owner = :owner")
    suspend fun count(owner: String): Int

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun find(id: String): CategoryEntity?

    /** Filas con cambios locales sin subir (para el push del sync). */
    @Query("SELECT * FROM categories WHERE owner = :owner AND dirty = 1")
    suspend fun dirtyRows(owner: String): List<CategoryEntity>

    @Query("UPDATE categories SET dirty = 0, remoteUpdated = :remoteUpdated WHERE id = :id")
    suspend fun markSynced(id: String, remoteUpdated: String)

    @Upsert
    suspend fun upsert(category: CategoryEntity)

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET deleted = 1, dirty = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDelete(id: String, updatedAt: Long)
}
