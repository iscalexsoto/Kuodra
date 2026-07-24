package com.arenacun.kuodra.presentation.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.data.mapper.toSettlementRecord
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SettlementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    settlementRepository: SettlementRepository,
) : ViewModel() {

    private val space = spaceRepository.activeSpace.value

    private val _uiState = MutableStateFlow(HistoryDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        if (space.useCase == UseCase.Personal) {
            _uiState.update { it.copy(record = settingsRepository.historyEntry(id)) }
        } else viewModelScope.launch {
            val record = settlementRepository.settlements(space.id).first()
                .firstOrNull { it.id == id }?.toSettlementRecord()
            _uiState.update { it.copy(record = record) }
        }
    }

    fun onReshare() = _uiState.update { it.copy(sheet = ReshareSheet.Options) }
    fun onShare() = _uiState.update { it.copy(sheet = ReshareSheet.Shared) }
    fun onCloseSheet() = _uiState.update { it.copy(sheet = ReshareSheet.None) }
}
