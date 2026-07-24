package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Settlement
import kotlinx.coroutines.flow.Flow

/** Cortes/liquidaciones de un espacio de Gastos. */
interface SettlementRepository {
    fun settlements(spaceId: String): Flow<List<Settlement>>

    /**
     * Registra un corte de periodo: estampa los movimientos liquidados y marca los pagos vivos
     * indicados como consumidos por el corte, para que dejen de contar en los balances.
     */
    suspend fun close(settlement: Settlement, movementIds: List<String>, paymentIds: List<String> = emptyList())

    /** Registra un pago individual (kind=Payment). Ajusta el saldo sin estampar movimientos. */
    suspend fun record(payment: Settlement)
}
