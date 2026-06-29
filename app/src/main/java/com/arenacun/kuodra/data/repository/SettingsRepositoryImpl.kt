package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.KuodraSeedSource
import com.arenacun.kuodra.data.mapper.toSettlementRecord
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SnapshotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Ajustes del espacio. **Personal** persiste el presupuesto vía [BudgetRepository] y el historial
 * vía [SnapshotRepository] (Room + sync); el resto de campos Personal (nombre implícito) y los
 * ajustes de Gastos/Caja siguen en memoria (seed) hasta que entren en alcance.
 */
class SettingsRepositoryImpl(
    private val seed: KuodraSeedSource,
    private val budgetRepository: BudgetRepository,
    private val snapshotRepository: SnapshotRepository,
) : SettingsRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Gastos/Caja: ajustes en memoria (sin persistencia aún). */
    private val others = mutableMapOf<UseCase, MutableStateFlow<SpaceSettings>>()

    private fun otherFlow(useCase: UseCase): MutableStateFlow<SpaceSettings> =
        others.getOrPut(useCase) { MutableStateFlow(seed.settings(useCase)) }

    /** Personal: base del seed con el presupuesto persistido superpuesto. */
    private val personal: StateFlow<SpaceSettings> = budgetRepository.budget
        .map { budget -> seed.settings(UseCase.Personal).copy(budget = budget) }
        .stateIn(scope, SharingStarted.Eagerly, seed.settings(UseCase.Personal))

    override fun settings(useCase: UseCase): StateFlow<SpaceSettings> =
        if (useCase == UseCase.Personal) personal else otherFlow(useCase)

    override fun update(useCase: UseCase, settings: SpaceSettings) {
        if (useCase == UseCase.Personal) {
            settings.budget?.let { budget -> scope.launch { budgetRepository.update(budget) } }
        } else {
            otherFlow(useCase).value = settings
        }
    }

    override fun history(useCase: UseCase): List<SettlementRecord> =
        if (useCase == UseCase.Personal) snapshotRepository.snapshots.value.map { it.toSettlementRecord() }
        else seed.history(useCase)

    override fun historyEntry(useCase: UseCase, id: String): SettlementRecord? =
        history(useCase).find { it.id == id }
}
