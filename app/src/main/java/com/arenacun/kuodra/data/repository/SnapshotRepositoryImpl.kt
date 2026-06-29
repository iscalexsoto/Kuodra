package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.PeriodSnapshotDao
import com.arenacun.kuodra.data.mapper.toDomain
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.PeriodSnapshot
import com.arenacun.kuodra.domain.repository.SnapshotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Periodos cerrados respaldados en Room (fuente de verdad) y sincronizados. */
@OptIn(ExperimentalCoroutinesApi::class)
class SnapshotRepositoryImpl(
    private val dao: PeriodSnapshotDao,
    private val sessionStore: SessionStore,
    private val syncTrigger: SyncTrigger,
) : SnapshotRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val snapshots: StateFlow<List<PeriodSnapshot>> =
        sessionStore.sessionFlow.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else dao.observe(session.userId).map { rows -> rows.map { it.toDomain() } }
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override suspend fun add(snapshot: PeriodSnapshot) {
        val owner = sessionStore.userId() ?: return
        dao.upsert(snapshot.toEntity(owner, System.currentTimeMillis(), dirty = true))
        syncTrigger.requestSync()
    }
}
