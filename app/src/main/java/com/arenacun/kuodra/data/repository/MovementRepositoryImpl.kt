package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.KuodraSeedSource
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.MovementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class MovementRepositoryImpl(
    private val seed: KuodraSeedSource,
) : MovementRepository {

    override fun movements(useCase: UseCase): Flow<List<Movement>> =
        combine(seed.deletedIds, seed.addedMovements) { deleted, added ->
            (seed.baseMovements(useCase) + added[useCase].orEmpty())
                .filter { it.id !in deleted }
        }

    override fun movement(useCase: UseCase, id: String): Movement? =
        (seed.baseMovements(useCase) + seed.addedFor(useCase)).find { it.id == id }

    override fun delete(id: String) = seed.markDeleted(id)

    override fun add(useCase: UseCase, movement: Movement) = seed.addMovement(useCase, movement)
}
