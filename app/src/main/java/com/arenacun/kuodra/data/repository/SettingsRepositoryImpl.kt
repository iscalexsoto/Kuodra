package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.mapper.toSettlementRecord
import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SnapshotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Ajustes Personal: presupuesto vía [BudgetRepository] e historial vía [SnapshotRepository]
 * (Room + sync), sobre una base propia. Los ajustes de Gastos viven en Space/Person repos.
 */
class SettingsRepositoryImpl(
    private val budgetRepository: BudgetRepository,
    private val snapshotRepository: SnapshotRepository,
) : SettingsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val personal: StateFlow<SpaceSettings> = budgetRepository.budget
        .map { budget -> PERSONAL_BASE.copy(budget = budget) }
        .stateIn(scope, SharingStarted.Eagerly, PERSONAL_BASE)

    override fun settings(): StateFlow<SpaceSettings> = personal

    override fun updateBudget(budget: BudgetConfig) {
        scope.launch { budgetRepository.update(budget) }
    }

    override fun history(): List<SettlementRecord> =
        snapshotRepository.snapshots.value.map { it.toSettlementRecord() }

    override fun historyEntry(id: String): SettlementRecord? =
        history().find { it.id == id }

    private companion object {
        /** Base Personal sin dependencia del seed; el presupuesto real se superpone. */
        val PERSONAL_BASE = SpaceSettings(
            name = "",
            members = emptyList(),
            budget = BudgetConfig.Default,
            reminderEnabled = false,
        )
    }
}
