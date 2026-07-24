package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.db.MovementDao
import com.arenacun.kuodra.data.mapper.toDomain
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.repository.MovementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Movimientos: Room como fuente de verdad (offline), filtrado por `owner` + `space` (`""` = Personal)
 * y con escrituras marcadas `dirty` que dispara el sync.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovementRepositoryImpl(
    private val dao: MovementDao,
    private val sessionStore: SessionStore,
    private val syncTrigger: SyncTrigger,
) : MovementRepository {

    override fun movements(spaceId: String): Flow<List<Movement>> =
        sessionStore.sessionFlow.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else dao.observe(session.userId, spaceId).map { rows -> rows.map { it.toDomain() } }
        }

    override suspend fun movement(id: String): Movement? =
        dao.find(id)?.takeIf { !it.deleted }?.toDomain()

    override suspend fun add(movement: Movement) = upsertLocal(movement)
    override suspend fun update(movement: Movement) = upsertLocal(movement)

    override suspend fun delete(id: String) {
        dao.softDelete(id, System.currentTimeMillis())
        syncTrigger.requestSync()
    }

    private suspend fun upsertLocal(movement: Movement) {
        val owner = sessionStore.userId() ?: return
        dao.upsert(movement.toEntity(owner, System.currentTimeMillis(), dirty = true))
        syncTrigger.requestSync()
    }
}
