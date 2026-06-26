package com.arenacun.kuodra.presentation.feature.movement

import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.MovementCategory
import java.time.LocalDate

/** Hoja inferior abierta en el alta (categoría / pagador / dividir). */
enum class AddSheet { Category, Payer, Split }

/**
 * Estado del formulario de alta de movimiento. Inmutable y expuesto por
 * [AddMovementViewModel]; los overlays (calculadora, calendario, sheets) viven aquí, no en
 * `remember`, para que la lógica suba al ViewModel (UDF).
 */
data class AddMovementUiState(
    val concept: String = "",
    val date: LocalDate = LocalDate.now(),
    val today: LocalDate = LocalDate.now(),
    /** Monto confirmado desde la calculadora (null = aún sin capturar). */
    val amount: Double? = null,
    /** Estado de trabajo de la calculadora mientras el diálogo está abierto. */
    val calc: CalcState = CalcState(),
    val category: MovementCategory = MovementCategory.defaults.first(),
    val categories: List<MovementCategory> = MovementCategory.defaults,
    val payer: String = "Tú",
    /** Personas seleccionadas para dividir (gastos). */
    val splitNames: List<String> = emptyList(),
    /** Candidatos del espacio (para pagador y dividir). */
    val members: List<String> = emptyList(),
    val sheet: AddSheet? = null,
    val showCalculator: Boolean = false,
    val showCalendar: Boolean = false,
) {
    val amountLabel: String get() = amount?.let { Calc.formatAmount(it) } ?: "$0"
    val hasAmount: Boolean get() = amount != null
}
