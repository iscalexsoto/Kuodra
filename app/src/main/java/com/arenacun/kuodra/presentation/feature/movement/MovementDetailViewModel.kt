package com.arenacun.kuodra.presentation.feature.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    private val personRepository: PersonRepository,
) : ViewModel() {

    private val space = spaceRepository.activeSpace.value
    private val useCase = space.useCase

    private val _uiState = MutableStateFlow(MovementDetailUiState())
    val uiState = _uiState.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    init {
        // Observa el flujo (no carga one-shot) para reflejar ediciones al volver del formulario.
        // Al borrar, el flujo emite sin el movimiento antes del popBackStack: se retiene el último
        // no nulo para evitar el flash de "no encontrado"; ese estado solo aplica si ya la
        // primera emisión viene sin el movimiento.
        viewModelScope.launch {
            val categories = summaryRepository.categories().associateBy { it.id }
            combine(
                movementRepository.movements(space.id),
                personRepository.persons(space.id),
            ) { list, people ->
                list.find { it.id == id } to people.associate { it.id to it.name }
            }.collect { (movement, persons) ->
                val ui = movement?.toUi(categories, useCase, LocalDate.now(), persons)
                _uiState.update { st ->
                    st.copy(movement = ui ?: st.movement, loading = false)
                }
            }
        }
    }

    fun onDeleteRequest() = _uiState.update { it.copy(confirmDelete = true) }

    fun onCancelDelete() = _uiState.update { it.copy(confirmDelete = false) }

    fun onConfirmDelete() {
        _uiState.update { it.copy(confirmDelete = false) }
        viewModelScope.launch {
            movementRepository.delete(id)
            _deleted.send(Unit)
        }
    }
}
