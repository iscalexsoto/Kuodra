package com.arenacun.kuodra.presentation.feature.allmovements

import com.arenacun.kuodra.domain.usecase.MovementFilter
import com.arenacun.kuodra.domain.usecase.MovementGroup

/** Estado de la pantalla "Ver todo los movimientos" (`scrVerTodo`). */
data class AllMovementsUiState(
    val groups: List<MovementGroup> = emptyList(),
    val filter: MovementFilter = MovementFilter(),
    /** Categorías presentes (para chips rápidos y sheet de filtros). */
    val allCategories: List<String> = emptyList(),
    /** Responsables presentes (para el sheet de filtros). */
    val allResponsibles: List<String> = emptyList(),
    val showSearch: Boolean = false,
    val showFilter: Boolean = false,
    val totalCount: Int = 0,
    val shownCount: Int = 0,
)
