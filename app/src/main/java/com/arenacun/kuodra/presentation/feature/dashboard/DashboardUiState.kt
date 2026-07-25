package com.arenacun.kuodra.presentation.feature.dashboard

import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.presentation.feature.movement.MovementUi

data class DashboardUiState(
    val space: Space = Space.PERSONAL,
    val movements: List<MovementUi> = emptyList(),
    val people: List<Person> = emptyList(),
    val categories: List<CategoryBreakdown> = emptyList(),
    /** Datos del hero para Personal (data-driven). Null en Gastos. */
    val personalHero: PersonalHero? = null,
    /** Datos del hero para Gastos (balances). Null en Personal. */
    val gastosHero: GastosHero? = null,
    /** Subtítulo de miembros del encabezado (Gastos): "Solo tú" / "N miembros". Null en Personal. */
    val membersLabel: String? = null,
    /** Gastos: hay saldos vivos sin liquidar (para el banner de recordatorio). */
    val hasUnsettledBalances: Boolean = false,
) {
    val useCase: UseCase get() = space.useCase
}

/** Hero del dashboard Gastos: tu saldo neto + totales que te deben / debes. */
data class GastosHero(
    val netLabel: String,
    val owedLabel: String,
    val oweLabel: String,
    val positive: Boolean,
)

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

/**
 * Hoja inferior abierta en el dashboard:
 * - [Spaces] selector "Tus espacios" (al tocar el título).
 * - [Menu] menú de acciones del espacio actual (botón ···).
 * - [PCloseConfirm] confirmación de cerrar periodo (Personal); [PClosed] éxito.
 * - [AddOptions] las 3 vías de alta al tocar el FAB "Agregar" (escanear/galería/manual).
 */
enum class DashboardSheet {
    None, Spaces, Menu, PCloseConfirm, PClosed, AddOptions,
}

/** Estado de los overlays del dashboard: hoja inferior activa. */
data class DashboardOverlay(
    val sheet: DashboardSheet = DashboardSheet.None,
)
