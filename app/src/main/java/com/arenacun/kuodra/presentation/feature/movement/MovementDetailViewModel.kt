package com.arenacun.kuodra.presentation.feature.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MovementDetailUiState(
    val movement: MovementUi? = null,
    val loading: Boolean = true,
    val confirmDelete: Boolean = false,
)

class MovementDetailViewModel(
    private val id: String,
    spaceRepository: SpaceRepository,
    summaryRepository: SummaryRepository,
    private val movementRepository: MovementRepository,
) : ViewModel() {

    private val useCase = spaceRepository.activeSpace.value.useCase

    private val _uiState = MutableStateFlow(MovementDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    init {
        viewModelScope.launch {
            val categories = summaryRepository.categories().associateBy { it.id }
            val movement = movementRepository.movement(useCase, id)
                ?.toUi(categories, useCase, LocalDate.now())
            _uiState.update { it.copy(movement = movement, loading = false) }
        }
    }

    fun onDeleteRequest() = _uiState.update { it.copy(confirmDelete = true) }

    fun onCancelDelete() = _uiState.update { it.copy(confirmDelete = false) }

    fun onConfirmDelete() {
        _uiState.update { it.copy(confirmDelete = false) }
        viewModelScope.launch {
            movementRepository.delete(useCase, id)
            _deleted.send(Unit)
        }
    }
}
