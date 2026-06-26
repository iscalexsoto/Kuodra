package com.arenacun.kuodra.presentation.feature.movement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcKey
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.MovementCategory
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
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
 * el evento [saved] para que la pantalla vuelva al dashboard.
 */
class AddMovementViewModel(
    spaceRepository: SpaceRepository,
    summaryRepository: SummaryRepository,
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
        AddMovementUiState(members = members, splitNames = members),
    )
    val uiState: StateFlow<AddMovementUiState> = _uiState.asStateFlow()

    private val _saved = Channel<Unit>(Channel.BUFFERED)
    val saved = _saved.receiveAsFlow()

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
    fun onCloseSheet() = _uiState.update { it.copy(sheet = null) }
    fun onPickCategory(category: MovementCategory) =
        _uiState.update { it.copy(category = category, sheet = null) }
    fun onPickPayer(name: String) = _uiState.update { it.copy(payer = name, sheet = null) }
    fun onToggleSplit(name: String) = _uiState.update { st ->
        val names = if (name in st.splitNames) st.splitNames - name else st.splitNames + name
        st.copy(splitNames = names)
    }

    fun onSave() {
        movementRepository.add(useCase, buildMovement(_uiState.value))
        viewModelScope.launch { _saved.send(Unit) }
    }

    private fun buildMovement(st: AddMovementUiState): Movement {
        val amountStr = st.amount?.let { Calc.formatAmount(it) } ?: "$0"
        val isShared = useCase != UseCase.Personal
        val byVerb = when (useCase) {
            UseCase.Gastos -> if (st.payer == "Tú") "Pagaste" else "Pagó"
            UseCase.Caja -> if (st.payer == "Tú") "Reportaste" else "Reportó"
            UseCase.Personal -> null
        }
        val split = if (useCase == UseCase.Gastos) st.splitNames else emptyList()
        val perHead = if (split.isNotEmpty() && st.amount != null)
            Calc.formatAmount(st.amount / split.size) else null
        val meta = buildString {
            if (isShared && byVerb != null) append("$byVerb ${st.payer} · ")
            append(DateLabels.dayMonth(st.date))
        }
        return Movement(
            id = "new-${System.currentTimeMillis()}",
            title = st.concept.ifBlank { st.category.name },
            meta = meta,
            amount = amountStr,
            catTag = st.category.tag,
            catName = st.category.name,
            tone = st.category.tone,
            dateStr = DateLabels.longLabel(st.date, st.today),
            by = if (isShared) st.payer else null,
            byVerb = byVerb,
            splitNames = split,
            perHead = perHead,
            note = "",
            date = st.date,
        )
    }
}
