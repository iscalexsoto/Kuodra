package com.arenacun.kuodra.presentation.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.data.mapper.toSettlementRecord
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SettlementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Lista de periodos cerrados (`scrHistory`). Personal lee los snapshots; Gastos lee los cortes del
 * [SettlementRepository].
 */
class HistoryViewModel(
    spaceRepository: SpaceRepository,
    settingsRepository: SettingsRepository,
    settlementRepository: SettlementRepository,
) : ViewModel() {
    private val space = spaceRepository.activeSpace.value

    /** Caso de uso del espacio activo: adapta título y vacío (cortes Personal vs liquidaciones Gastos). */
    val useCase: UseCase = space.useCase

    val records: StateFlow<List<SettlementRecord>> =
        if (space.useCase == UseCase.Personal) {
            flowOf(settingsRepository.history())
        } else {
            settlementRepository.settlements(space.id).map { list -> list.map { it.toSettlementRecord() } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
