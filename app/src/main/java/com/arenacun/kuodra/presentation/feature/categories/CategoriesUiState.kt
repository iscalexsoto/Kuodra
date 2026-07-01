package com.arenacun.kuodra.presentation.feature.categories

import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.presentation.component.CategoryDraft

/**
 * Estado de la pantalla de Categorías: catálogo filtrado por [query] (incluye la estática
 * "Sin categoría", siempre primera) y el borrador de creación/edición en curso.
 */
data class CategoriesUiState(
    val categories: List<Category> = emptyList(),
    val query: String = "",
    /** Borrador de creación/edición de categoría (null = ninguno). */
    val editingCategory: CategoryDraft? = null,
)
