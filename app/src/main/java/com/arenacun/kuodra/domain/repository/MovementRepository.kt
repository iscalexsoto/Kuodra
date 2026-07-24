package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Movement
import kotlinx.coroutines.flow.Flow

/** Movimientos de un espacio (`""` = Personal). Observable para reflejar altas y bajas en la UI. */
interface MovementRepository {
    /** Movimientos vigentes (excluye los eliminados) del espacio. */
    fun movements(spaceId: String): Flow<List<Movement>>

    /** Busca un movimiento por id (los ids son globales). */
    suspend fun movement(id: String): Movement?

    /** Agrega un movimiento (su `spaceId` decide a qué espacio pertenece). */
    suspend fun add(movement: Movement)

    /** Actualiza un movimiento existente. */
    suspend fun update(movement: Movement)

    /** Marca un movimiento como eliminado. */
    suspend fun delete(id: String)
}
