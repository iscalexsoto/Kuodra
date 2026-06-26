package com.arenacun.kuodra.presentation.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PreferencesRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val spaceRepository: SpaceRepository,
    movementRepository: MovementRepository,
    summaryRepository: SummaryRepository,
    private val preferences: PreferencesRepository,
) : ViewModel() {

    val uiState = spaceRepository.activeSpace
        .flatMapLatest { space ->
            movementRepository.movements(space.useCase).map { movements ->
                DashboardUiState(
                    space = space,
                    movements = movements,
                    people = summaryRepository.people(space.useCase),
                    categories = summaryRepository.categories(),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val menu = MutableStateFlow(DashboardOverlay())

    val overlay = combine(menu, preferences.darkTheme) { m, dark -> m.copy(darkTheme = dark) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardOverlay())

    fun onToggleTheme() = preferences.toggleTheme()

    // ---- Hojas inferiores (espacios / crear / menú) ----
    fun onOpenSpaces() = menu.update { it.copy(sheet = DashboardSheet.Spaces) }
    fun onOpenCreateSpace() = menu.update { it.copy(sheet = DashboardSheet.CreateSpace) }
    fun onOpenMenu() = menu.update { it.copy(sheet = DashboardSheet.Menu) }
    fun onCloseSheet() = menu.update { it.copy(sheet = DashboardSheet.None) }

    /** Cambia al espacio (caso de uso) elegido y cierra el selector. */
    fun onSelectUseCase(useCase: UseCase) {
        spaceRepository.selectUseCase(useCase)
        menu.update { it.copy(sheet = DashboardSheet.None) }
    }

    // ---- Salir / archivar grupo ----
    fun onLeaveStart() = menu.update { it.copy(sheet = DashboardSheet.None, leaveStep = LeaveStep.Settle) }
    fun onLeaveAdvance() = menu.update {
        it.copy(leaveStep = when (it.leaveStep) {
            LeaveStep.Settle -> LeaveStep.Confirm
            LeaveStep.Confirm -> LeaveStep.Done
            else -> it.leaveStep
        })
    }
    fun onLeaveClose() = menu.update { it.copy(leaveStep = LeaveStep.None) }
}
