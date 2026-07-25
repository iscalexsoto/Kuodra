package com.arenacun.kuodra.presentation.feature.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcKey
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.MovementItem
import com.arenacun.kuodra.domain.model.PayerShare
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitRule
import com.arenacun.kuodra.domain.model.SplitShare
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.newId
import com.arenacun.kuodra.domain.scan.TicketScan
import com.arenacun.kuodra.domain.repository.CategoryRepository
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.usecase.SplitCalc
import com.arenacun.kuodra.domain.usecase.SplitRuleCalc
import com.arenacun.kuodra.presentation.component.CategoryDraft
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Alta y edición de movimiento. Recoge el formulario (monto vía calculadora, fecha vía calendario,
 * categoría/pagador/dividir vía sheets) y al guardar inserta o actualiza en [MovementRepository] y
 * emite el evento [saved] para que la pantalla vuelva atrás. El catálogo de categorías es
 * reactivo ([CategoryRepository]) y se pueden crear categorías nuevas inline. Con [editId] el
 * formulario se pre-puebla desde el movimiento existente y guardar conserva su id; si el
 * movimiento no existe, degrada a alta nueva.
 */
class AddMovementViewModel(
    private val editId: String?,
    private val spaceRepository: SpaceRepository,
    personRepository: PersonRepository,
    private val categoryRepository: CategoryRepository,
    private val movementRepository: MovementRepository,
) : ViewModel() {

    /** true si se navegó con intención de editar (aunque la pre-carga aún no termine). */
    val isEditMode: Boolean = editId != null

    val space: StateFlow<Space> = spaceRepository.activeSpace
    private val useCase: UseCase = space.value.useCase
    private val spaceId: String = space.value.id

    private val _uiState = MutableStateFlow(
        AddMovementUiState(
            category = Category.Uncategorized,
            categories = categoryRepository.categories.value,
        ),
    )
    val uiState: StateFlow<AddMovementUiState> = _uiState.asStateFlow()

    init {
        // Miembros del espacio (Gastos): "Tú" ([PersonRef.ME]) + contactos, y la regla de división por
        // defecto (que puede cambiar en Ajustes durante la sesión). El prellenado se recalcula en cada
        // emisión mientras nadie haya tocado la división: así es idempotente y converge sea cual sea el
        // orden en que lleguen las personas y la pre-carga de edición.
        if (useCase == UseCase.Gastos) viewModelScope.launch {
            combine(spaceRepository.activeSpace, personRepository.persons(spaceId)) { sp, people ->
                sp to people
            }.collect { (space, people) ->
                val members = listOf(SpacePerson(PersonRef.ME, "Tú")) + people
                val ids = members.map { it.id }
                val rule = if (space.splitRule.enabled) SplitRuleCalc.sanitize(space.splitRule, ids)
                else SplitRuleCalc.implicitDefault(ids)
                _uiState.update { st ->
                    val base = st.copy(members = members, rule = rule)
                    if (st.splitTouched) base else applyRule(base, rule)
                }
            }
        }
    }

    /** Prellena la división desde la regla. Idempotente; no marca [AddMovementUiState.splitTouched]. */
    private fun applyRule(st: AddMovementUiState, rule: SplitRule): AddMovementUiState = st.copy(
        splitMode = rule.mode,
        splitIds = rule.participantIds.toSet(),
        percentDraft = rule.shares.associate { it.personId to it.percent },
        payers = listOf(PayerShare(rule.payerId, st.total)),
    )

    /** Descarta los ajustes de este gasto y vuelve al prellenado de la regla del espacio. */
    fun onResetSplitToRule() = _uiState.update { applyRule(it, it.rule).copy(splitTouched = false) }

    private val _saved = Channel<SaveResult>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

    /** Movimiento Personal derivable de la última alta compartida (tu parte), o null. */
    private var personalCopy: Movement? = null

    init {
        // Catálogo reactivo: refleja categorías creadas (aquí o en Ajustes). Al llegar el
        // catálogo, re-resuelve la selección actual por id (cierra la carrera con la pre-carga
        // de edición, que puede correr antes de que el catálogo esté poblado).
        viewModelScope.launch {
            categoryRepository.categories.collect { cats ->
                _uiState.update { st ->
                    st.copy(
                        categories = cats,
                        category = cats.find { it.id == st.category.id } ?: st.category,
                    )
                }
            }
        }
        if (editId != null) viewModelScope.launch {
            movementRepository.movement(editId)?.let { m ->
                _uiState.update { st ->
                    st.copy(
                        concept = m.title,
                        amount = m.amount.major.takeIf { it != 0.0 },
                        date = m.date,
                        // Preserva el categoryId original aunque la categoría ya no exista en el
                        // catálogo (o este aún no cargue): solo cambia si el usuario elige otra.
                        category = st.categories.find { it.id == m.categoryId }
                            ?: Category.byId(m.categoryId).takeIf { it.id == m.categoryId }
                            ?: Category.Uncategorized.copy(id = m.categoryId),
                        payers = m.payers.ifEmpty { st.payers },
                        splitMode = if (m.splitMode != SplitMode.None) m.splitMode else st.splitMode,
                        splitIds = m.splits.map { it.personId }.toSet().ifEmpty { st.splitIds },
                        amountDraft = m.splits.associate { it.personId to it.share.cents },
                        percentDraft = if (m.amount.cents > 0)
                            m.splits.associate { it.personId to Math.round(it.share.cents * 100.0 / m.amount.cents).toInt() }
                        else emptyMap(),
                        items = m.items,
                        note = m.note,
                        scanRawText = m.scanRawText,
                        scanSource = m.scanSource,
                        // La división guardada gana: congela el prellenado para que la regla del
                        // espacio no la pise cuando lleguen las personas.
                        splitTouched = m.splits.isNotEmpty(),
                        isEditing = true,
                    )
                }
            }
        }
    }

    fun onConceptChange(value: String) = _uiState.update { it.copy(concept = value) }

    /**
     * Pre-población desde un escaneo de ticket. Solo pisa lo que el parseo detectó; la fecha se
     * acota a `today` (el calendario no permite futuro). El raw OCR y el origen viajan en el
     * estado hasta [onSave] para persistirse con el movimiento.
     */
    fun applyScan(scan: TicketScan) = _uiState.update { st ->
        val p = scan.parsed
        st.copy(
            concept = p.merchant ?: st.concept,
            amount = p.total?.major ?: st.amount,
            date = p.date?.coerceAtMost(st.today) ?: st.date,
            items = p.items.map { MovementItem(newId(), it.concept, it.amount) },
            scanRawText = scan.rawText,
            scanSource = scan.scanSource,
        )
    }

    // ---- Fecha ----
    fun onPickToday() = _uiState.update { it.copy(date = it.today) }
    fun onPickYesterday() = _uiState.update { it.copy(date = it.today.minusDays(1)) }
    fun onOpenCalendar() = _uiState.update { it.copy(showCalendar = true) }
    fun onDismissCalendar() = _uiState.update { it.copy(showCalendar = false) }
    fun onPickDate(date: LocalDate) = _uiState.update { it.copy(date = date, showCalendar = false) }

    // ---- Calculadora ----
    fun onOpenCalculator() = _uiState.update { it.copy(showCalculator = true, calc = Calc.initial(it.amount)) }
    fun onCalcKey(key: CalcKey) = _uiState.update { it.copy(calc = Calc.press(it.calc, key)) }
    fun onDismissCalculator() = _uiState.update { it.copy(showCalculator = false) }
    fun onConfirmAmount() = _uiState.update { st ->
        val amount = st.calc.result ?: st.amount
        // Con un solo pagador (el caso por defecto, sea "Tú" o quien diga la regla) su aporte sigue al
        // total: así al añadir un segundo pagador se arranca desde "total / 0", no desde "0 / 0".
        val payers = if (st.payers.size == 1)
            listOf(st.payers.first().copy(amount = amount?.let { Money.ofMajor(it) } ?: Money.Zero))
        else st.payers
        st.copy(amount = amount, payers = payers, showCalculator = false)
    }

    // ---- Pantalla de división (pagadores + reparto) ----
    // Todo cambio manual marca `splitTouched`: la regla del espacio deja de re-aplicarse sobre él.
    fun onTogglePayer(id: String) = _uiState.update { st ->
        val isPayer = st.payers.any { it.personId == id }
        val payers = if (isPayer)
            st.payers.filterNot { it.personId == id }
        else st.payers + PayerShare(id, Money.Zero)
        // Al añadir un pagador, se auto-incluye como participante de la división (removible
        // aparte). Al quitarlo como pagador, su participación en el reparto se deja intacta.
        val splitIds = if (isPayer) st.splitIds else st.splitIds + id
        st.copy(payers = payers, splitIds = splitIds, splitTouched = true)
    }
    fun onSetPayerAmount(id: String, cents: Long) = _uiState.update { st ->
        st.copy(
            payers = st.payers.map { if (it.personId == id) it.copy(amount = Money(cents)) else it },
            splitTouched = true,
        )
    }
    fun onSetSplitMode(mode: SplitMode) = _uiState.update { it.copy(splitMode = mode, splitTouched = true) }
    fun onToggleSplitMember(id: String) = _uiState.update { st ->
        val ids = if (id in st.splitIds) st.splitIds - id else st.splitIds + id
        st.copy(splitIds = ids, splitTouched = true)
    }
    fun onSetSplitAmount(id: String, cents: Long) = _uiState.update { st ->
        st.copy(amountDraft = st.amountDraft + (id to cents), splitTouched = true)
    }
    fun onSetSplitPercent(id: String, percent: Int) = _uiState.update { st ->
        st.copy(percentDraft = st.percentDraft + (id to percent.coerceIn(0, 100)), splitTouched = true)
    }

    // ---- Number pad de la pantalla de división (monto pagador / monto split / porcentaje) ----
    /** Abre el number pad para el campo indicado, precargando su valor actual. */
    fun onOpenSplitPad(target: SplitPadTarget) = _uiState.update { st ->
        val current = when (target.kind) {
            SplitPadKind.PayerAmount -> (st.payers.firstOrNull { it.personId == target.personId }?.amount?.cents ?: 0L) / 100.0
            SplitPadKind.SplitAmount -> (st.amountDraft[target.personId] ?: 0L) / 100.0
            SplitPadKind.SplitPercent -> (st.percentDraft[target.personId] ?: 0).toDouble()
        }
        st.copy(splitPadTarget = target, splitPad = Calc.initial(current))
    }
    fun onSplitPadKey(key: CalcKey) = _uiState.update { it.copy(splitPad = Calc.press(it.splitPad, key)) }
    fun onDismissSplitPad() = _uiState.update { it.copy(splitPadTarget = null) }
    fun onConfirmSplitPad() = _uiState.update { st ->
        val target = st.splitPadTarget ?: return@update st.copy(splitPadTarget = null)
        val result = st.splitPad.result ?: 0.0
        val id = target.personId
        val next = when (target.kind) {
            SplitPadKind.PayerAmount ->
                st.copy(payers = st.payers.map { if (it.personId == id) it.copy(amount = Money.ofMajor(result)) else it })
            SplitPadKind.SplitAmount ->
                st.copy(amountDraft = st.amountDraft + (id to Money.ofMajor(result).cents))
            SplitPadKind.SplitPercent ->
                st.copy(percentDraft = st.percentDraft + (id to result.toInt().coerceIn(0, 100)))
        }
        next.copy(splitPadTarget = null, splitTouched = true)
    }

    // ---- Hojas de selección de la pantalla de división ----
    fun onOpenSplitSheet(sheet: SplitSheet) = _uiState.update { it.copy(splitSheet = sheet) }
    fun onCloseSplitSheet() = _uiState.update { it.copy(splitSheet = null) }

    // ---- Sheets ----
    fun onOpenSheet(sheet: AddSheet) = _uiState.update { it.copy(sheet = sheet) }
    fun onCloseSheet() = _uiState.update { st ->
        // Al cerrar, descarta las partidas totalmente vacías (sin concepto ni cantidad).
        val items = st.items.filter { it.amount.cents != 0L || it.concept.isNotBlank() }
        st.copy(sheet = null, editingCategory = null, items = items)
    }
    fun onPickCategory(category: Category) =
        _uiState.update { it.copy(category = category, sheet = null) }

    // ---- Detalle (partidas): pantalla propia ----
    /** Al salir de la pantalla de detalle, descarta las partidas totalmente vacías (como onCloseSheet). */
    fun onCloseDetail() = _uiState.update { st ->
        st.copy(items = st.items.filter { it.amount.cents != 0L || it.concept.isNotBlank() })
    }

    // ---- Detalle (partidas) ----
    fun onAddItem() = _uiState.update { st ->
        st.copy(items = st.items + MovementItem(newId(), "", Money.Zero))
    }
    fun onItemConcept(id: String, value: String) = _uiState.update { st ->
        st.copy(items = st.items.map { if (it.id == id) it.copy(concept = value) else it })
    }
    fun onRemoveItem(id: String) = _uiState.update { st ->
        st.copy(items = st.items.filterNot { it.id == id })
    }

    // ---- Teclado numérico (cantidad de una partida) ----
    fun onOpenItemPad(id: String) = _uiState.update { st ->
        val cents = st.items.firstOrNull { it.id == id }?.amount?.cents ?: 0L
        // Precarga el monto actual como valor a editar (fresh); vacío si es cero.
        st.copy(showNumberPad = true, editingItemId = id, pad = Calc.initial(cents / 100.0))
    }
    fun onPadKey(key: CalcKey) = _uiState.update { it.copy(pad = Calc.press(it.pad, key)) }
    fun onDismissPad() = _uiState.update { it.copy(showNumberPad = false, editingItemId = null) }
    fun onConfirmItemAmount() = _uiState.update { st ->
        val id = st.editingItemId ?: return@update st.copy(showNumberPad = false)
        val amount = st.pad.result?.let { Money.ofMajor(it) } ?: Money.Zero
        st.copy(
            items = st.items.map { if (it.id == id) it.copy(amount = amount) else it },
            showNumberPad = false,
            editingItemId = null,
        )
    }

    // ---- Crear categoría inline ----
    fun onStartCreateCategory() = _uiState.update { it.copy(editingCategory = CategoryDraft()) }
    fun onCategoryDraftName(value: String) =
        _uiState.update { it.copy(editingCategory = it.editingCategory?.copy(name = value)) }
    fun onCategoryDraftTone(tone: AvatarTone) =
        _uiState.update { it.copy(editingCategory = it.editingCategory?.copy(tone = tone)) }
    fun onCancelCreateCategory() = _uiState.update { it.copy(editingCategory = null) }

    fun onConfirmCreateCategory() {
        val draft = _uiState.value.editingCategory ?: return
        if (draft.name.isBlank()) return
        val category = Category(
            id = newId(),
            name = draft.name.trim(),
            tag = Category.deriveTag(draft.name),
            tone = draft.tone,
        )
        viewModelScope.launch { categoryRepository.add(category) }
        // Selecciona la nueva categoría; la lista se actualiza vía el flujo reactivo.
        _uiState.update { it.copy(category = category, editingCategory = null) }
    }

    fun onSave() {
        val st = _uiState.value
        val movement = buildMovement(st)
        viewModelScope.launch {
            if (st.isEditing) movementRepository.update(movement)
            else movementRepository.add(movement)
            // Alta nueva de Gastos donde "Tú" participa ⇒ registrar tu parte como gasto Personal. El
            // guard `!isEditing` es esencial: sin él, cada guardado de una edición duplicaría la copia.
            val myShare = if (!st.isEditing && useCase == UseCase.Gastos)
                movement.splits.firstOrNull { it.personId == PersonRef.ME }?.share else null
            if (myShare != null && myShare.cents > 0L) {
                val copy = Movement(
                    id = newId(),
                    amount = myShare,
                    categoryId = movement.categoryId,
                    title = movement.title,
                    note = "De ${space.value.displayName}",
                    date = movement.date,
                    spaceId = "",
                )
                val shareLabel = Calc.formatAmount(myShare.major)
                if (st.rule.autoPersonalCopy) {
                    // La regla del espacio ya lo autorizó: se registra sin diálogo.
                    movementRepository.add(copy)
                    _saved.send(SaveResult.PersonalCopySaved(shareLabel))
                } else {
                    personalCopy = copy
                    _saved.send(SaveResult.OfferPersonalCopy(shareLabel))
                }
            } else {
                _saved.send(SaveResult.Done)
            }
        }
    }

    /** Registra la copia Personal (tu parte) preparada en [onSave]. */
    fun onConfirmPersonalCopy() {
        val copy = personalCopy ?: return
        personalCopy = null
        viewModelScope.launch { movementRepository.add(copy) }
    }

    private fun buildMovement(st: AddMovementUiState): Movement {
        val amount = st.amount?.let { Money.ofMajor(it) } ?: Money.Zero
        val gastos = useCase == UseCase.Gastos
        val items = st.items.filter { it.amount.cents != 0L || it.concept.isNotBlank() }
        return Movement(
            id = if (st.isEditing) editId!! else newId(),
            amount = amount,
            categoryId = st.category.id,
            title = st.concept.ifBlank { st.category.name },
            note = st.note,
            date = st.date,
            spaceId = spaceId,
            payers = if (gastos) resolvePayers(st, amount) else emptyList(),
            splitMode = if (gastos) st.splitMode else SplitMode.None,
            splits = if (gastos) resolveSplits(st, amount) else emptyList(),
            items = items,
            scanRawText = st.scanRawText,
            scanSource = st.scanSource,
        )
    }

    /** Un pagador único siempre cubre el total; con varios se respetan sus montos. */
    private fun resolvePayers(st: AddMovementUiState, total: Money): List<PayerShare> =
        if (st.payers.size == 1) listOf(st.payers.first().copy(amount = total)) else st.payers

    /** Resuelve la división del estado a centavos exactos según el modo elegido. */
    private fun resolveSplits(st: AddMovementUiState, total: Money): List<SplitShare> {
        val ids = st.activeSplitIds
        return when (st.splitMode) {
            SplitMode.Equal -> SplitCalc.resolveEqual(total, ids)
            SplitMode.Amount -> ids.map { SplitShare(it, Money(st.amountDraft[it] ?: 0L)) }
            SplitMode.Percent -> SplitCalc.resolvePercents(total, ids.map { it to (st.percentDraft[it] ?: 0) })
            SplitMode.None -> emptyList()
        }
    }

    /** Error de validación de los pagadores (null = cuadra). Un pagador único siempre cubre el total. */
    fun payersError(st: AddMovementUiState): String? =
        if (st.payers.size == 1) null else SplitCalc.validatePayers(st.total, st.payers)

    /** Error de validación de la división según el modo (null = cuadra). */
    fun splitError(st: AddMovementUiState): String? {
        val ids = st.activeSplitIds
        return when (st.splitMode) {
            SplitMode.Equal -> if (ids.isEmpty()) "Selecciona al menos una persona" else null
            SplitMode.Amount -> SplitCalc.validateAmounts(st.total, ids.map { SplitShare(it, Money(st.amountDraft[it] ?: 0L)) })
            SplitMode.Percent -> SplitCalc.validatePercents(ids.map { it to (st.percentDraft[it] ?: 0) })
            SplitMode.None -> null
        }
    }
}

/** Resultado del guardado: cerrar sin más, u ofrecer/registrar tu parte como gasto Personal. */
sealed interface SaveResult {
    data object Done : SaveResult
    data class OfferPersonalCopy(val shareLabel: String) : SaveResult

    /** La copia Personal se registró sola (regla del espacio con `autoPersonalCopy`); sin diálogo. */
    data class PersonalCopySaved(val shareLabel: String) : SaveResult
}
