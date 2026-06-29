package com.arenacun.kuodra.presentation.feature.settings

import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.presentation.component.CategoryDraft

/** Campo monetario que está editando la calculadora dentro de ajustes. */
enum class CalcTarget { Budget, Fund }

/** Borrador del sheet de agregar/editar contacto (`addContact` del prototipo). */
data class ContactDraft(
    /** Nombre original si se está editando; null si es nuevo. */
    val original: String?,
    val name: String,
    val whatsapp: String,
)

data class SettingsUiState(
    val useCase: UseCase = UseCase.Personal,
    val settings: SpaceSettings? = null,
    val darkTheme: Boolean = false,
    val calcTarget: CalcTarget? = null,
    val calc: CalcState = CalcState(),
    val editingContact: ContactDraft? = null,
    /** Catálogo de categorías del usuario (Personal); incluye la estática "Sin categoría". */
    val categories: List<Category> = emptyList(),
    /** Borrador de creación/edición de categoría (null = ninguno). */
    val editingCategory: CategoryDraft? = null,
)
