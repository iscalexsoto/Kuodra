package com.arenacun.kuodra.presentation.feature.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcKey
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.newId
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
 * Alta de movimiento. Recoge el formulario (monto vía calculadora, fecha vía calendario,
 * categoría/pagador/dividir vía sheets) y al guardar inserta en [MovementRepository] y emite
 * el evento [saved] para que la pantalla vuelva al dashboard. El catálogo de categorías es
 * reactivo ([CategoryRepository]) y se pueden crear categorías nuevas inline.
 */
class AddMovementViewModel(
    spaceRepository: SpaceRepository,
    summaryRepository: SummaryRepository,
    private val categoryRepository: CategoryRepository,
    private val movementRepository: MovementRepository,
) : ViewModel() {

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
        // Catálogo reactivo: refleja categorías creadas (aquí o en Ajustes).
        viewModelScope.launch {
            categoryRepository.categories.collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun onConceptChange(value: String) = _uiState.update { it.copy(concept = value) }

    // ---- Fecha ----
    fun onPickToday() = _uiState.update { it.copy(date = it.today) }
    fun onPickYesterday() = _uiState.update { it.copy(date = it.today.minusDays(1)) }
    fun onOpenCalendar() = _uiState.update { it.copy(showCalendar = true) }
    fun onDismissCalendar() = _uiState.update { it.copy(showCalendar = false) }
    fun onPickDate(date: LocalDate) = _uiState.update { it.copy(date = date, showCalendar = false) }

    // ---- Calculadora ----
    fun onOpenCalculator() = _uiState.update { it.copy(showCalculator = true, calc = CalcState()) }
    fun onCalcKey(key: CalcKey) = _uiState.update { it.copy(calc = Calc.press(it.calc, key)) }
    fun onDismissCalculator() = _uiState.update { it.copy(showCalculator = false) }
    fun onConfirmAmount() = _uiState.update { st ->
        st.copy(amount = st.calc.result ?: st.amount, showCalculator = false)
    }

    // ---- Sheets ----
    fun onOpenSheet(sheet: AddSheet) = _uiState.update { it.copy(sheet = sheet) }
    fun onCloseSheet() = _uiState.update { it.copy(sheet = null, editingCategory = null) }
    fun onPickCategory(category: Category) =
        _uiState.update { it.copy(category = category, sheet = null) }
    fun onPickPayer(name: String) = _uiState.update { it.copy(payer = name, sheet = null) }
    fun onToggleSplit(name: String) = _uiState.update { st ->
        val names = if (name in st.splitNames) st.splitNames - name else st.splitNames + name
        st.copy(splitNames = names)
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
        val movement = buildMovement(_uiState.value)
        viewModelScope.launch {
            movementRepository.add(useCase, movement)
            _saved.send(Unit)
        }
    }

    private fun buildMovement(st: AddMovementUiState): Movement {
        val amount = st.amount?.let { Money.ofMajor(it) } ?: Money.Zero
        val payer = if (useCase != UseCase.Personal) st.payer else null
        val split = if (useCase == UseCase.Gastos) st.splitNames else emptyList()
        return Movement(
            id = newId(),
            amount = amount,
            categoryId = st.category.id,
            title = st.concept.ifBlank { st.category.name },
            note = "",
            date = st.date,
            payer = payer,
            splitNames = split,
        )
    }
}
