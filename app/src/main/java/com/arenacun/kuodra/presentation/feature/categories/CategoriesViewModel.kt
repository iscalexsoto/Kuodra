package com.arenacun.kuodra.presentation.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.newId
import com.arenacun.kuodra.domain.repository.CategoryRepository
import com.arenacun.kuodra.presentation.component.CategoryDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Gestión del catálogo de categorías (pantalla dedicada con buscador). Lee el
 * [CategoryRepository] reactivo y combina con el estado local de UI (texto de búsqueda y
 * borrador de creación/edición). La estática "Sin categoría" no se puede editar/borrar.
 */
class CategoriesViewModel(
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private data class Local(
        val query: String = "",
        val editingCategory: CategoryDraft? = null,
    )

    private val local = MutableStateFlow(Local())

    val uiState = combine(categoryRepository.categories, local) { categories, l ->
        val q = l.query.trim()
        val filtered = if (q.isEmpty()) categories
        else categories.filter { it.name.contains(q, ignoreCase = true) }
        CategoriesUiState(filtered, l.query, l.editingCategory)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    fun onQueryChange(value: String) = local.update { it.copy(query = value) }

    fun onStartCreateCategory() = local.update { it.copy(editingCategory = CategoryDraft()) }
    fun onEditCategory(category: Category) = local.update {
        it.copy(editingCategory = CategoryDraft(category, category.name, category.tone))
    }
    fun onCategoryDraftName(value: String) =
        local.update { it.copy(editingCategory = it.editingCategory?.copy(name = value)) }
    fun onCategoryDraftTone(tone: AvatarTone) =
        local.update { it.copy(editingCategory = it.editingCategory?.copy(tone = tone)) }
    fun onCloseCategory() = local.update { it.copy(editingCategory = null) }

    fun onConfirmCategory() {
        val draft = local.value.editingCategory ?: return
        if (draft.name.isBlank()) return
        val original = draft.original
        val category = (original ?: Category(id = newId(), name = "", tag = "", tone = draft.tone)).copy(
            name = draft.name.trim(),
            tag = Category.deriveTag(draft.name),
            tone = draft.tone,
        )
        viewModelScope.launch {
            if (original == null) categoryRepository.add(category) else categoryRepository.update(category)
        }
        local.update { it.copy(editingCategory = null) }
    }

    fun onDeleteCategory() {
        val original = local.value.editingCategory?.original ?: return
        viewModelScope.launch { categoryRepository.delete(original.id) }
        local.update { it.copy(editingCategory = null) }
    }
}
