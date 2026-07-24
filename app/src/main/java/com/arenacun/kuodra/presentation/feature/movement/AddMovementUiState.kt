package com.arenacun.kuodra.presentation.feature.movement

import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.ReturnStatus
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.adjustmentOf
import com.arenacun.kuodra.domain.scan.ScanSource
import com.arenacun.kuodra.presentation.component.CategoryDraft
import java.time.LocalDate

/** Hoja inferior abierta en el alta (categoría / detalle). Pagador y división viven en pantalla propia. */
enum class AddSheet { Category, Detail }

/**
 * Estado del formulario de alta de movimiento. Inmutable y expuesto por [AddMovementViewModel]; los
 * overlays (calculadora, calendario, sheets) viven aquí, no en `remember`, para que la lógica suba
 * al ViewModel (UDF). Los pagadores y la división (Gastos) se editan en `SplitConfigScreen`.
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
    /** Candidatos del espacio (Gastos): "Tú" ([PersonRef.ME]) + contactos. */
    val members: List<SpacePerson> = listOf(SpacePerson(PersonRef.ME, "Tú")),
    /** Pagadores del gasto (Gastos). Por defecto "Tú" paga todo. */
    val payers: List<PayerShare> = listOf(PayerShare(PersonRef.ME, Money.Zero)),
    /** Modo de división elegido (Gastos). */
    val splitMode: SplitMode = SplitMode.Equal,
    /** Ids de quienes participan en la división. Se rellena con todos los miembros al cargar. */
    val splitIds: Set<String> = emptySet(),
    /** Montos exactos por persona (modo Amount), en centavos. */
    val amountDraft: Map<String, Long> = emptyMap(),
    /** Porcentajes por persona (modo Percent). */
    val percentDraft: Map<String, Int> = emptyMap(),
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
    /** Raw OCR del escaneo que pre-pobló el formulario (no se pinta; viaja al guardar). */
    val scanRawText: String? = null,
    /** Origen del escaneo; null = captura manual. */
    val scanSource: ScanSource? = null,
    /** Nota original del movimiento en edición (sin UI; se preserva al guardar). */
    val note: String = "",
    /** Estado de devolución (Personal). Se alterna con el FieldRow "Devolución". */
    val returnStatus: ReturnStatus = ReturnStatus.None,
    /** % congelado si el movimiento en edición ya estaba devuelto; se preserva al guardar. */
    val returnPercent: Int? = null,
    /** true si el formulario pre-cargó un movimiento existente (guardar = actualizar). */
    val isEditing: Boolean = false,
) {
    val amountLabel: String get() = amount?.let { Calc.formatAmount(it) } ?: "$0"
    val hasAmount: Boolean get() = amount != null

    /** Total capturado como [Money] (0 si aún no hay monto). */
    val total: Money get() = amount?.let { Money.ofMajor(it) } ?: Money.Zero

    /** Remanente no detallado: total − suma de partidas. */
    val adjustment: Money get() = adjustmentOf(total, items)

    /** Nombre a partir de un id de miembro. */
    fun memberName(id: String): String = members.firstOrNull { it.id == id }?.name ?: id

    /** Resumen para el FieldRow de división: "Pagaste $500 · entre 4, equitativo". */
    val splitSummary: String
        get() {
            val payerLabel = when {
                payers.isEmpty() -> "Sin pagador"
                payers.size == 1 && payers.first().personId == PersonRef.ME -> "Pagaste tú"
                payers.size == 1 -> "Pagó ${memberName(payers.first().personId)}"
                else -> "${payers.size} pagadores"
            }
            val modeLabel = when (splitMode) {
                SplitMode.Equal -> "equitativo"
                SplitMode.Amount -> "por montos"
                SplitMode.Percent -> "por porcentajes"
                SplitMode.None -> "sin dividir"
            }
            return "$payerLabel · entre ${splitIds.size}, $modeLabel"
        }
}
