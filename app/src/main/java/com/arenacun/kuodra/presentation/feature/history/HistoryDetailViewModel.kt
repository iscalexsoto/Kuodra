package com.arenacun.kuodra.presentation.feature.history

import androidx.lifecycle.ViewModel
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Sheet del flujo de reenvío de un corte (`reshare` → `shared` del prototipo). */
enum class ReshareSheet { None, Options, Shared }

data class HistoryDetailUiState(
    val record: SettlementRecord? = null,
    val sheet: ReshareSheet = ReshareSheet.None,
)

class HistoryDetailViewModel(
    id: String,
    spaceRepository: SpaceRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val useCase = spaceRepository.activeSpace.value.useCase

    private val _uiState = MutableStateFlow(
        HistoryDetailUiState(record = settingsRepository.historyEntry(useCase, id)),
    )
    val uiState = _uiState.asStateFlow()

    fun onReshare() = _uiState.update { it.copy(sheet = ReshareSheet.Options) }
    fun onShare() = _uiState.update { it.copy(sheet = ReshareSheet.Shared) }
    fun onCloseSheet() = _uiState.update { it.copy(sheet = ReshareSheet.None) }
}
