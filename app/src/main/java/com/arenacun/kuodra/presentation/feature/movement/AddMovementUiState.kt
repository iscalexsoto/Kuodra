package com.arenacun.kuodra.presentation.feature.movement

import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.adjustmentOf
import com.arenacun.kuodra.presentation.component.CategoryDraft
import java.time.LocalDate

/** Hoja inferior abierta en el alta (categoría / pagador / dividir / detalle). */
enum class AddSheet { Category, Payer, Split, Detail }

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
    val category: Category = Category.Uncategorized,
    val categories: List<Category> = listOf(Category.Uncategorized),
    val payer: String = "Tú",
    /** Personas seleccionadas para dividir (gastos). */
    val splitNames: List<String> = emptyList(),
    /** Candidatos del espacio (para pagador y dividir). */
    val members: List<String> = emptyList(),
    val sheet: AddSheet? = null,
    /** Borrador de nueva categoría dentro del selector (null = no se está creando). */
    val editingCategory: CategoryDraft? = null,
    /** Desglose en partidas (concepto + cantidad). Vacío = sin detalle. */
    val items: List<MovementItem> = emptyList(),
    /** Partida cuyo monto se edita en el teclado numérico (null = ninguno). */
    val editingItemId: String? = null,
    /** Estado de trabajo del teclado numérico mientras el diálogo está abierto. */
    val pad: CalcState = CalcState(),
    val showCalculator: Boolean = false,
    val showCalendar: Boolean = false,
    val showNumberPad: Boolean = false,
) {
    val amountLabel: String get() = amount?.let { Calc.formatAmount(it) } ?: "$0"
    val hasAmount: Boolean get() = amount != null

    /** Total capturado como [Money] (0 si aún no hay monto). */
    val total: Money get() = amount?.let { Money.ofMajor(it) } ?: Money.Zero

    /** Remanente no detallado: total − suma de partidas. */
    val adjustment: Money get() = adjustmentOf(total, items)
}
