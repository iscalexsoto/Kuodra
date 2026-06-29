package com.arenacun.kuodra.data.local.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodSnapshotDao {

    @Query("SELECT * FROM period_snapshots WHERE owner = :owner AND deleted = 0 ORDER BY periodStart DESC")
    fun observe(owner: String): Flow<List<PeriodSnapshotEntity>>

    @Query("SELECT * FROM period_snapshots WHERE id = :id")
    suspend fun find(id: String): PeriodSnapshotEntity?

    @Query("SELECT * FROM period_snapshots WHERE owner = :owner AND dirty = 1")
    suspend fun dirtyRows(owner: String): List<PeriodSnapshotEntity>

    @Query("UPDATE period_snapshots SET dirty = 0, remoteUpdated = :remoteUpdated WHERE id = :id")
    suspend fun markSynced(id: String, remoteUpdated: String)

    @Upsert
    suspend fun upsert(snapshot: PeriodSnapshotEntity)
}
