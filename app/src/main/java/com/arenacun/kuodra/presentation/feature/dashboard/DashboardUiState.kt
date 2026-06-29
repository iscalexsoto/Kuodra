package com.arenacun.kuodra.presentation.feature.dashboard

import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.presentation.feature.movement.MovementUi

data class DashboardUiState(
    val space: Space = Space(UseCase.Gastos),
    val movements: List<MovementUi> = emptyList(),
    val people: List<Person> = emptyList(),
    val categories: List<CategoryBreakdown> = emptyList(),
    /** Datos del hero para Personal (data-driven). Null en Gastos/Caja (hero aún hardcodeado). */
    val personalHero: PersonalHero? = null,
) {
    val useCase: UseCase get() = space.useCase
}

/**
 * Hero del dashboard Personal. Sin presupuesto activo muestra solo [totalLabel] (gasto del mes);
 * con presupuesto añade [budget] (progreso y ritmo del periodo).
 */
data class PersonalHero(
    val totalLabel: String,
    val caption: String,
    val budget: BudgetHero? = null,
)

data class BudgetHero(
    val frequencyBadge: String,
    val progressLabel: String,
    val rightLabel: String,
    val pct: Float,
    val onTrack: Boolean,
    val paceText: String,
    val paceDetail: String,
)

/**
 * Desglose por categoría del dashboard personal, **computado** a partir de los movimientos del
 * espacio (antes era seed display). Reemplaza al antiguo `Category` con campos de presentación.
 */
data class CategoryBreakdown(
    val name: String,
    val sub: String,
    val amount: String,
    val pct: Float,
    val tag: String,
    val tone: AvatarTone,
)

/** Paso del flujo de salir/archivar grupo (`leaveOpen` del prototipo). */
enum class LeaveStep { None, Settle, Confirm, Done }

/**
 * Hoja inferior abierta en el dashboard:
 * - [Spaces] selector "Tus espacios" (al tocar el título).
 * - [CreateSpace] opciones de tipo de espacio a crear.
 * - [Menu] menú de acciones del espacio actual (botón ···).
 * - [Share] opciones de compartir resumen/corte (PDF/WhatsApp); [Shared] confirmación.
 * - [PCloseConfirm] confirmación de cerrar periodo (Personal); [PClosed] éxito.
 */
enum class DashboardSheet { None, Spaces, CreateSpace, Menu, Share, Shared, PCloseConfirm, PClosed }

/** Estado de los overlays del dashboard: hoja inferior activa y flujo de archivar. */
data class DashboardOverlay(
    val sheet: DashboardSheet = DashboardSheet.None,
    val leaveStep: LeaveStep = LeaveStep.None,
)
