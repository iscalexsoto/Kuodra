package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.UseCase
import kotlinx.coroutines.flow.Flow

/** Movimientos del espacio. Observable para reflejar altas y bajas en la UI. */
interface MovementRepository {
    /** Movimientos vigentes (excluye los eliminados) del caso de uso. */
    fun movements(useCase: UseCase): Flow<List<Movement>>

    /** Busca un movimiento por id dentro del caso de uso. */
    fun movement(useCase: UseCase, id: String): Movement?

    /** Marca un movimiento como eliminado. */
    fun delete(id: String)

    /** Agrega un movimiento al caso de uso indicado. */
    fun add(useCase: UseCase, movement: Movement)
}
