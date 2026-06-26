package com.arenacun.kuodra.presentation.feature.history

import androidx.lifecycle.ViewModel
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository

/** Lista de periodos cerrados (`scrHistory`). */
class HistoryViewModel(
    spaceRepository: SpaceRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val useCase = spaceRepository.activeSpace.value.useCase
    val records: List<SettlementRecord> = settingsRepository.history(useCase)
}
