package com.arenacun.kuodra.presentation.feature.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.ReturnStatus
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import com.arenacun.kuodra.domain.usecase.ReturnCalc
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
    /** true en Personal: habilita la tarjeta y acciones de devolución. */
    val returnsEnabled: Boolean = false,
    /** Estado de devolución del movimiento actual, para decidir qué acción mostrar. */
    val returnStatus: ReturnStatus = ReturnStatus.None,
)

class MovementDetailViewModel(
    private val id: String,
    spaceRepository: SpaceRepository,
    summaryRepository: SummaryRepository,
    private val movementRepository: MovementRepository,
    private val settingsRepository: SettingsRepository,
    private val personRepository: PersonRepository,
) : ViewModel() {

    private val space = spaceRepository.activeSpace.value
    private val useCase = space.useCase

    private val _uiState = MutableStateFlow(MovementDetailUiState(returnsEnabled = useCase == UseCase.Personal))
    val uiState = _uiState.asStateFlow()

    private val _deleted = Channel<Unit>(Channel.BUFFERED)
    val deleted = _deleted.receiveAsFlow()

    /** Movimiento crudo vigente, para construir las copias de las acciones de devolución. */
    private var current: Movement? = null

    init {
        // Observa el flujo (no carga one-shot) para reflejar ediciones al volver del formulario.
        // Al borrar, el flujo emite sin el movimiento antes del popBackStack: se retiene el último
        // no nulo para evitar el flash de "no encontrado"; ese estado solo aplica si ya la
        // primera emisión viene sin el movimiento.
        viewModelScope.launch {
            val categories = summaryRepository.categories().associateBy { it.id }
            combine(
                movementRepository.movements(space.id),
                settingsRepository.settings(),
                personRepository.persons(space.id),
            ) { list, settings, people ->
                val percent = settings.budget?.returnPercent ?: ReturnCalc.DEFAULT_RETURN_PERCENT
                Triple(list.find { it.id == id }, percent, people.associate { it.id to it.name })
            }.collect { (movement, percent, persons) ->
                current = movement ?: current
                val ui = movement?.toUi(categories, useCase, LocalDate.now(), percent, persons)
                _uiState.update { st ->
                    st.copy(
                        movement = ui ?: st.movement,
                        loading = false,
                        returnStatus = current?.returnStatus ?: st.returnStatus,
                    )
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

    /** Marca "Por devolver" un movimiento que no participaba (o reabre uno devuelto). */
    fun onSetPending() = changeReturn(ReturnStatus.Pending, stamp = false)

    /** Quita el movimiento de las devoluciones. */
    fun onSetNone() = changeReturn(ReturnStatus.None, stamp = false)

    /** Marca "Devuelto": congela el % global vigente en el movimiento. */
    fun onMarkReturned() = changeReturn(ReturnStatus.Returned, stamp = true)

    /** Reabre una devolución ya cerrada: vuelve a "Por devolver" y limpia la estampa. */
    fun onReopenReturn() = changeReturn(ReturnStatus.Pending, stamp = false)

    private fun changeReturn(status: ReturnStatus, stamp: Boolean) {
        val movement = current ?: return
        viewModelScope.launch {
            val stampedPercent = if (stamp)
                settingsRepository.settings().value.budget?.returnPercent
                    ?: ReturnCalc.DEFAULT_RETURN_PERCENT
            else null
            movementRepository.update(
                movement.copy(returnStatus = status, returnPercent = stampedPercent),
            )
        }
    }
}
