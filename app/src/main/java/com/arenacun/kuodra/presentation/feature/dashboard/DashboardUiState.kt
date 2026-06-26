package com.arenacun.kuodra.presentation.feature.dashboard

import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase

data class DashboardUiState(
    val space: Space = Space(UseCase.Gastos),
    val movements: List<Movement> = emptyList(),
    val people: List<Person> = emptyList(),
    val categories: List<Category> = emptyList(),
) {
    val useCase: UseCase get() = space.useCase
}

/** Paso del flujo de salir/archivar grupo (`leaveOpen` del prototipo). */
enum class LeaveStep { None, Settle, Confirm, Done }

/**
 * Hoja inferior abierta en el dashboard:
 * - [Spaces] selector "Tus espacios" (al tocar el título).
 * - [CreateSpace] opciones de tipo de espacio a crear.
 * - [Menu] menú de acciones del espacio actual (botón ···).
 */
enum class DashboardSheet { None, Spaces, CreateSpace, Menu }

/** Estado de los overlays del dashboard: hoja inferior activa y flujo de archivar. */
data class DashboardOverlay(
    val sheet: DashboardSheet = DashboardSheet.None,
    val leaveStep: LeaveStep = LeaveStep.None,
    val darkTheme: Boolean = false,
)
