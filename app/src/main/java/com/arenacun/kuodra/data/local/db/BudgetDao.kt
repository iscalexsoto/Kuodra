package com.arenacun.kuodra.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budget WHERE owner = :owner")
    fun observe(owner: String): Flow<BudgetEntity?>

    @Query("SELECT * FROM budget WHERE owner = :owner AND dirty = 1")
    suspend fun dirtyRows(owner: String): List<BudgetEntity>

    @Query("UPDATE budget SET dirty = 0, remoteUpdated = :remoteUpdated WHERE owner = :owner")
    suspend fun markSynced(owner: String, remoteUpdated: String)

    @Upsert
    suspend fun upsert(budget: BudgetEntity)
}
