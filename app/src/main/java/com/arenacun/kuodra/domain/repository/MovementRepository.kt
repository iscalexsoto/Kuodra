package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.UseCase
import kotlinx.coroutines.flow.Flow

/** Movimientos del espacio. Observable para reflejar altas y bajas en la UI. */
interface MovementRepository {
    /** Movimientos vigentes (excluye los eliminados) del caso de uso. */
    fun movements(useCase: UseCase): Flow<List<Movement>>

    /** Busca un movimiento por id dentro del caso de uso. */
    suspend fun movement(useCase: UseCase, id: String): Movement?

    /** Agrega un movimiento al caso de uso indicado. */
    suspend fun add(useCase: UseCase, movement: Movement)

    /** Actualiza un movimiento existente. */
    suspend fun update(useCase: UseCase, movement: Movement)

    /** Marca un movimiento como eliminado. */
    suspend fun delete(useCase: UseCase, id: String)
}
