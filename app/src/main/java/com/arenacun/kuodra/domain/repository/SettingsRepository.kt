package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.SpaceSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * Ajustes Personal (presupuesto + devoluciones) e historial de cortes Personal. Los ajustes de un
 * espacio de Gastos (nombre, recordatorio, contactos) viven en [SpaceRepository]/[PersonRepository].
 */
interface SettingsRepository {
    /** Ajustes Personal, observables (solo `budget` es relevante). */
    fun settings(): StateFlow<SpaceSettings>

    fun updateBudget(budget: BudgetConfig)

    /** Historial de periodos cerrados (snapshots Personal). */
    fun history(): List<SettlementRecord>

    /** Un registro del historial por id. */
    fun historyEntry(id: String): SettlementRecord?
}
