package com.arenacun.kuodra.presentation.feature.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MovementDetailUiState(
    val movement: Movement? = null,
    val confirmDelete: Boolean = false,
)

class MovementDetailViewModel(
    private val id: String,
    spaceRepository: SpaceRepository,
    private val movementRepository: MovementRepository,
) : ViewModel() {

    private val useCase = spaceRepository.activeSpace.value.useCase

    private val _uiState = MutableStateFlow(
        MovementDetailUiState(movement = movementRepository.movement(useCase, id)),
    )
    val uiState = _uiState.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    fun onDeleteRequest() = _uiState.update { it.copy(confirmDelete = true) }

    fun onCancelDelete() = _uiState.update { it.copy(confirmDelete = false) }

    fun onConfirmDelete() {
        movementRepository.delete(id)
        _uiState.update { it.copy(confirmDelete = false) }
        viewModelScope.launch { _deleted.send(Unit) }
    }
}
