package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.local.KuodraSeedSource
import com.arenacun.kuodra.data.local.db.MovementDao
import com.arenacun.kuodra.data.mapper.toDomain
import com.arenacun.kuodra.data.mapper.toEntity
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.MovementRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Movimientos. **Personal** es real: Room como fuente de verdad (offline), filtrado por `owner` y
 * con escrituras marcadas `dirty` (las sube el sync de Fase 2). **Gastos/Caja** siguen sobre el
 * seed en memoria (fuera de alcance de esta versión); migran al mismo patrón cuando entren.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovementRepositoryImpl(
    private val seed: KuodraSeedSource,
    private val dao: MovementDao,
    private val sessionStore: SessionStore,
    private val syncTrigger: SyncTrigger,
) : MovementRepository {

    private val personalMovements: Flow<List<Movement>> =
        sessionStore.sessionFlow.flatMapLatest { session ->
            if (session == null) flowOf(emptyList())
            else dao.observe(session.userId).map { rows -> rows.map { it.toDomain() } }
        }

    override fun movements(useCase: UseCase): Flow<List<Movement>> = when (useCase) {
        UseCase.Personal -> personalMovements
        else -> combine(seed.deletedIds, seed.addedMovements) { deleted, added ->
            (seed.baseMovements(useCase) + added[useCase].orEmpty())
                .filter { it.id !in deleted }
        }
    }

    override suspend fun movement(useCase: UseCase, id: String): Movement? = when (useCase) {
        UseCase.Personal -> dao.find(id)?.takeIf { !it.deleted }?.toDomain()
        else -> (seed.baseMovements(useCase) + seed.addedFor(useCase)).find { it.id == id }
    }

    override suspend fun add(useCase: UseCase, movement: Movement) {
        if (useCase == UseCase.Personal) upsertLocal(movement) else seed.addMovement(useCase, movement)
    }

    override suspend fun update(useCase: UseCase, movement: Movement) {
        if (useCase == UseCase.Personal) upsertLocal(movement) else seed.addMovement(useCase, movement)
    }

    override suspend fun delete(useCase: UseCase, id: String) {
        if (useCase == UseCase.Personal) {
            dao.softDelete(id, System.currentTimeMillis())
            syncTrigger.requestSync()
        } else {
            seed.markDeleted(id)
        }
    }

    private suspend fun upsertLocal(movement: Movement) {
        val owner = sessionStore.userId() ?: return
        dao.upsert(movement.toEntity(owner, System.currentTimeMillis(), dirty = true))
        syncTrigger.requestSync()
    }
}
