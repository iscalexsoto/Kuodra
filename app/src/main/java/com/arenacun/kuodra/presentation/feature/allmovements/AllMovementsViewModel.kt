package com.arenacun.kuodra.presentation.feature.allmovements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.usecase.MovementFilter
import com.arenacun.kuodra.domain.usecase.MovementPeriod
import com.arenacun.kuodra.domain.usecase.MovementQuery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

/**
 * "Ver todo los movimientos": observa el repositorio y aplica búsqueda/filtros/agrupación
 * (lógica pura en [MovementQuery]). Los overlays (búsqueda, filtros) viven en el estado local.
 */
class AllMovementsViewModel(
    spaceRepository: SpaceRepository,
    movementRepository: MovementRepository,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val useCase = spaceRepository.activeSpace.value.useCase

    private data class Local(
        val filter: MovementFilter = MovementFilter(),
        val showSearch: Boolean = false,
        val showFilter: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    val uiState = combine(movementRepository.movements(useCase), local) { movements, l ->
        val filtered = MovementQuery.filter(movements, l.filter, today)
        AllMovementsUiState(
            groups = MovementQuery.groupByDay(filtered, today),
            filter = l.filter,
            allCategories = movements.map { it.catName }.distinct(),
            allResponsibles = movements.mapNotNull { it.by }.distinct(),
            showSearch = l.showSearch,
            showFilter = l.showFilter,
            totalCount = movements.size,
            shownCount = filtered.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AllMovementsUiState())

    // ---- Búsqueda ----
    fun onOpenSearch() = local.update { it.copy(showSearch = true) }
    fun onCloseSearch() = local.update { it.copy(showSearch = false, filter = it.filter.copy(query = "")) }
    fun onQueryChange(q: String) = local.update { it.copy(filter = it.filter.copy(query = q)) }

    // ---- Filtros ----
    fun onOpenFilter() = local.update { it.copy(showFilter = true) }
    fun onCloseFilter() = local.update { it.copy(showFilter = false) }

    fun onToggleCategory(name: String) = local.update { l ->
        val set = l.filter.categories.let { if (name in it) it - name else it + name }
        l.copy(filter = l.filter.copy(categories = set))
    }

    fun onSetPeriod(period: MovementPeriod) = local.update { l ->
        val next = if (l.filter.period == period) MovementPeriod.All else period
        l.copy(filter = l.filter.copy(period = next))
    }

    fun onToggleResponsible(name: String) = local.update { l ->
        val set = l.filter.responsibles.let { if (name in it) it - name else it + name }
        l.copy(filter = l.filter.copy(responsibles = set))
    }

    fun onClearFilters() = local.update { it.copy(filter = MovementFilter(query = it.filter.query)) }
}
