package com.arenacun.kuodra.presentation.feature.settings

import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.ThemeMode
import com.arenacun.kuodra.domain.model.UseCase

/** Campo monetario que está editando la calculadora dentro de ajustes. */
enum class CalcTarget { Budget }

/** Borrador del sheet de agregar/editar contacto (`addContact` del prototipo). */
data class ContactDraft(
    /** Id del contacto si se está editando; null si es nuevo. */
    val id: String?,
    val name: String,
    val whatsapp: String,
)

data class SettingsUiState(
    val useCase: UseCase = UseCase.Personal,
    val settings: SpaceSettings? = null,
    /** Contactos del espacio (Gastos): fuente para editar por id + teléfono. */
    val contacts: List<SpacePerson> = emptyList(),
    val themeMode: ThemeMode = ThemeMode.System,
    val calcTarget: CalcTarget? = null,
    val calc: CalcState = CalcState(),
    val editingContact: ContactDraft? = null,
    /** Nombre con el que el usuario quiere ser identificado (sección Cuenta). */
    val accountName: String = "",
    /** Borrador de edición del nombre de usuario (null = sheet cerrado). */
    val editingName: String? = null,
)
