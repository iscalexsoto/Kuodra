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
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.newId
import com.arenacun.kuodra.domain.scan.TicketScan
import com.arenacun.kuodra.domain.repository.CategoryRepository
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import com.arenacun.kuodra.presentation.component.CategoryDraft
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    spaceRepository: SpaceRepository,
    summaryRepository: SummaryRepository,
    private val categoryRepository: CategoryRepository,
    private val movementRepository: MovementRepository,
) : ViewModel() {

    /** true si se navegó con intención de editar (aunque la pre-carga aún no termine). */
    val isEditMode: Boolean = editId != null

    val space: StateFlow<Space> = spaceRepository.activeSpace
    private val useCase: UseCase = space.value.useCase

    /** Candidatos del espacio: "Tú" + personas (deduplicado). Vacío en Personal. */
    private val members: List<String> = buildList {
        add("Tú")
        addAll(summaryRepository.people(useCase).map { it.name }.filterNot { it == "Tú" })
    }

    private val _uiState = MutableStateFlow(
        AddMovementUiState(
            members = members,
            splitNames = members,
            category = Category.Uncategorized,
            categories = categoryRepository.categories.value,
        ),
    )
    val uiState: StateFlow<AddMovementUiState> = _uiState.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

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
            movementRepository.movement(useCase, editId)?.let { m ->
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
                        payer = m.payer ?: st.payer,
                        splitNames = if (useCase == UseCase.Gastos) m.splitNames else st.splitNames,
                        items = m.items,
                        note = m.note,
                        scanRawText = m.scanRawText,
                        scanSource = m.scanSource,
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
        st.copy(amount = st.calc.result ?: st.amount, showCalculator = false)
    }

    // ---- Sheets ----
    fun onOpenSheet(sheet: AddSheet) = _uiState.update { it.copy(sheet = sheet) }
    fun onCloseSheet() = _uiState.update { st ->
        // Al cerrar, descarta las partidas totalmente vacías (sin concepto ni cantidad).
        val items = st.items.filter { it.amount.cents != 0L || it.concept.isNotBlank() }
        st.copy(sheet = null, editingCategory = null, items = items)
    }
    fun onPickCategory(category: Category) =
        _uiState.update { it.copy(category = category, sheet = null) }
    fun onPickPayer(name: String) = _uiState.update { it.copy(payer = name, sheet = null) }
    fun onToggleSplit(name: String) = _uiState.update { st ->
        val names = if (name in st.splitNames) st.splitNames - name else st.splitNames + name
        st.copy(splitNames = names)
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
            if (st.isEditing) movementRepository.update(useCase, movement)
            else movementRepository.add(useCase, movement)
            _saved.send(Unit)
        }
    }

    private fun buildMovement(st: AddMovementUiState): Movement {
        val amount = st.amount?.let { Money.ofMajor(it) } ?: Money.Zero
        val payer = if (useCase != UseCase.Personal) st.payer else null
        val split = if (useCase == UseCase.Gastos) st.splitNames else emptyList()
        val items = st.items.filter { it.amount.cents != 0L || it.concept.isNotBlank() }
        return Movement(
            id = if (st.isEditing) editId!! else newId(),
            amount = amount,
            categoryId = st.category.id,
            title = st.concept.ifBlank { st.category.name },
            note = st.note,
            date = st.date,
            payer = payer,
            splitNames = split,
            items = items,
            scanRawText = st.scanRawText,
            scanSource = st.scanSource,
        )
    }
}
