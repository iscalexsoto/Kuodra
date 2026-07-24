package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

/** Cortes/liquidaciones de un espacio de Gastos. */
interface SettlementRepository {
    fun settlements(spaceId: String): Flow<List<Settlement>>

    /** Registra un corte y estampa los movimientos liquidados para que dejen de contar en balances. */
    suspend fun close(settlement: Settlement, movementIds: List<String>)
}
