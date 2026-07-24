package com.arenacun.kuodra.presentation.feature.allmovements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import com.arenacun.kuodra.domain.usecase.MovementFilter
import com.arenacun.kuodra.domain.usecase.MovementPeriod
import com.arenacun.kuodra.domain.usecase.MovementQuery
import com.arenacun.kuodra.presentation.feature.movement.toUi
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
    summaryRepository: SummaryRepository,
    personRepository: PersonRepository,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {

    private val space = spaceRepository.activeSpace.value
    private val useCase = space.useCase
    private val categories: Map<String, Category> = summaryRepository.categories().associateBy { it.id }

    private fun catName(m: Movement): String = (categories[m.categoryId] ?: Category.byId(m.categoryId)).name
    private fun payerNames(m: Movement, persons: Map<String, String>): List<String> =
        m.payers.map { if (it.personId == PersonRef.ME) "Tú" else persons[it.personId] ?: "(eliminado)" }

    private data class Local(
        val filter: MovementFilter = MovementFilter(),
        val showSearch: Boolean = false,
        val showFilter: Boolean = false,
    )

    private val local = MutableStateFlow(Local())

    val uiState = combine(
        movementRepository.movements(space.id),
        personRepository.persons(space.id),
        local,
    ) { movements, people, l ->
        val persons = people.associate { it.id to it.name }
        val filtered = MovementQuery.filter(movements, l.filter, today, { catName(it) }, { payerNames(it, persons) })
        AllMovementsUiState(
            groups = MovementQuery.groupByDay(filtered, today).map { it.toUi(categories, useCase, today, persons = persons) },
            filter = l.filter,
            allCategories = movements.map { catName(it) }.distinct(),
            allResponsibles = movements.flatMap { payerNames(it, persons) }.distinct(),
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
