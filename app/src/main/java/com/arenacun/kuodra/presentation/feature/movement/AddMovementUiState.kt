package com.arenacun.kuodra.presentation.feature.movement

import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitRule
import com.arenacun.kuodra.domain.model.adjustmentOf
import com.arenacun.kuodra.domain.scan.ScanSource
import com.arenacun.kuodra.presentation.component.CategoryDraft
import java.time.LocalDate

/** Hoja inferior abierta en el alta (categoría). Detalle, pagador y división viven en pantalla propia. */
enum class AddSheet { Category }

/** Tipo de monto que se edita en el number pad de la pantalla de división. */
enum class SplitPadKind { PayerAmount, SplitAmount, SplitPercent }

/** Campo concreto (tipo + persona) que se edita en el number pad de división (null = ninguno). */
data class SplitPadTarget(val kind: SplitPadKind, val personId: String)

/** Hoja inferior de selección en la pantalla de división: elegir pagadores / participantes. */
enum class SplitSheet { AddPayer, AddParticipant }

/**
 * Estado del formulario de alta de movimiento. Inmutable y expuesto por [AddMovementViewModel]; los
 * overlays (calculadora, calendario, sheets) viven aquí, no en `remember`, para que la lógica suba
 * al ViewModel (UDF). Los pagadores y la división (Gastos) se editan en `SplitConfigScreen`.
 */
data class AddMovementUiState(
    val concept: String = "",
    val today: LocalDate = LocalDate.now(),
    /** Un solo `now()`: si se leyera el reloj dos veces podrían caer en días distintos. */
    val date: LocalDate = today,
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
    /** Campo de monto/porcentaje que se edita con el number pad en la pantalla de división. */
    val splitPadTarget: SplitPadTarget? = null,
    /** Estado de trabajo del number pad de división mientras el diálogo está abierto. */
    val splitPad: CalcState = CalcState(),
    /** Hoja de selección abierta en la pantalla de división (null = ninguna). */
    val splitSheet: SplitSheet? = null,
    /**
     * Regla de división del espacio, ya saneada; fuente del prellenado. `SplitRule.Default` = el
     * espacio no tiene regla y se usa la heurística implícita.
     */
    val rule: SplitRule = SplitRule.Default,
    /**
     * true cuando la división ya la fijó el usuario (o la pre-carga de edición): la regla deja de
     * re-aplicarse. Distingue "aún sin prellenar" de "el usuario deseleccionó a todos".
     */
    val splitTouched: Boolean = false,
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

    /** true si la división visible es el prellenado de la regla del espacio (nadie la ha tocado). */
    val splitFromRule: Boolean get() = rule.enabled && !splitTouched

    /**
     * Participantes efectivos, en el orden de [members] (que es el que decide quién absorbe el centavo
     * de remanente al resolver la división). Intersecta con [members] en lugar de podar [splitIds]: así
     * un contacto que aún no ha cargado no borra la selección de un movimiento en edición.
     */
    val activeSplitIds: List<String> get() = members.map { it.id }.filter { it in splitIds }

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
            val suffix = if (splitFromRule) " · por defecto" else ""
            return "$payerLabel · entre ${activeSplitIds.size}, $modeLabel$suffix"
        }
}
