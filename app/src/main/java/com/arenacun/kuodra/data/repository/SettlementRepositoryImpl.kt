package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.MovementDao
import com.arenacun.kuodra.data.local.db.SettlementDao
import com.arenacun.kuodra.data.mapper.toDomain
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.repository.SettlementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/** Cortes de un espacio: Room como fuente de verdad. Cerrar estampa los movimientos liquidados. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettlementRepositoryImpl(
    private val dao: SettlementDao,
    private val movementDao: MovementDao,
    private val sessionStore: SessionStore,
    private val syncTrigger: SyncTrigger,
) : SettlementRepository {

    override fun settlements(spaceId: String): Flow<List<Settlement>> =
        sessionStore.sessionFlow.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else dao.observe(session.userId, spaceId).map { rows -> rows.map { it.toDomain() } }
        }

    override suspend fun close(settlement: Settlement, movementIds: List<String>) {
        val owner = sessionStore.userId() ?: return
        val now = System.currentTimeMillis()
        dao.upsert(settlement.toEntity(owner, now, dirty = true))
        if (movementIds.isNotEmpty()) movementDao.stampSettlement(movementIds, settlement.id, now)
        syncTrigger.requestSync()
    }
}
