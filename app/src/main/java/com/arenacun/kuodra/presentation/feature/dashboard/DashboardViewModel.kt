package com.arenacun.kuodra.presentation.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.total
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import com.arenacun.kuodra.domain.usecase.BudgetPeriod
import com.arenacun.kuodra.presentation.feature.movement.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val spaceRepository: SpaceRepository,
    movementRepository: MovementRepository,
    private val summaryRepository: SummaryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    val uiState = spaceRepository.activeSpace
        .flatMapLatest { space ->
            combine(
                movementRepository.movements(space.useCase),
                settingsRepository.settings(space.useCase),
            ) { movements, settings ->
                val categories = summaryRepository.categories().associateBy { it.id }
                DashboardUiState(
                    space = space,
                    movements = movements.map { it.toUi(categories, space.useCase, today) },
                    people = summaryRepository.people(space.useCase),
                    categories = breakdown(movements, categories),
                    personalHero = if (space.useCase == UseCase.Personal) personalHero(movements, settings) else null,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val menu = MutableStateFlow(DashboardOverlay())

    val overlay = menu.asStateFlow()

    // ---- Hojas inferiores (espacios / crear / menú) ----
    fun onOpenSpaces() = menu.update { it.copy(sheet = DashboardSheet.Spaces) }
    fun onOpenCreateSpace() = menu.update { it.copy(sheet = DashboardSheet.CreateSpace) }
    fun onOpenMenu() = menu.update { it.copy(sheet = DashboardSheet.Menu) }
    fun onCloseSheet() = menu.update { it.copy(sheet = DashboardSheet.None) }

    // ---- Compartir resumen/corte (Gastos/Caja) ----
    fun onShare() = menu.update { it.copy(sheet = DashboardSheet.Share) }
    fun onShareConfirm() = menu.update { it.copy(sheet = DashboardSheet.Shared) }

    // ---- Cerrar periodo (Personal) ----
    fun onClosePeriod() = menu.update { it.copy(sheet = DashboardSheet.PCloseConfirm) }
    fun onClosePeriodConfirm() = menu.update { it.copy(sheet = DashboardSheet.PClosed) }

    /** Cambia al espacio (caso de uso) elegido y cierra el selector. */
    fun onSelectUseCase(useCase: UseCase) {
        spaceRepository.selectUseCase(useCase)
        menu.update { it.copy(sheet = DashboardSheet.None) }
    }

    // ---- Salir / archivar grupo ----
    fun onLeaveStart() = menu.update { it.copy(sheet = DashboardSheet.None, leaveStep = LeaveStep.Settle) }
    fun onLeaveAdvance() = menu.update {
        it.copy(leaveStep = when (it.leaveStep) {
            LeaveStep.Settle -> LeaveStep.Confirm
            LeaveStep.Confirm -> LeaveStep.Done
            else -> it.leaveStep
        })
    }
    fun onLeaveClose() = menu.update { it.copy(leaveStep = LeaveStep.None) }

    /** Hero Personal: con presupuesto activo muestra progreso del periodo; si no, total del mes. */
    private fun personalHero(movements: List<Movement>, settings: SpaceSettings): PersonalHero {
        val budget = settings.budget
        if (budget != null && budget.enabled) {
            val window = BudgetPeriod.current(budget, today)
            val spent = movements.filter { window.contains(it.date) }.map { it.amount }.total()
            val budgetCents = Calc.parseAmount(budget.amount)?.let { Money.ofMajor(it).cents } ?: 0L
            val pct = if (budgetCents > 0) spent.cents.toFloat() / budgetCents else 0f
            val periodPct = window.elapsedFraction(today)
            val onTrack = pct <= periodPct + 0.001f
            return PersonalHero(
                totalLabel = Calc.formatAmount(spent.major),
                caption = "Gastos del periodo",
                budget = BudgetHero(
                    frequencyBadge = frequencyBadge(budget),
                    progressLabel = "${Calc.formatAmount(spent.major)} de ${budget.amount} presupuesto",
                    rightLabel = "${(pct * 100).roundToInt()}%",
                    pct = pct.coerceIn(0f, 1f),
                    onTrack = onTrack,
                    paceText = if (onTrack) "Vas a buen ritmo" else "Vas sobre el ritmo",
                    paceDetail = "${(pct * 100).roundToInt()}% del presupuesto · ${(periodPct * 100).roundToInt()}% del periodo",
                ),
            )
        }
        val month = YearMonth.from(today)
        val spent = movements.filter { YearMonth.from(it.date) == month }.map { it.amount }.total()
        return PersonalHero(
            totalLabel = Calc.formatAmount(spent.major),
            caption = "Gastos de ${DateLabels.monthName(today)}",
            budget = null,
        )
    }

    private fun frequencyBadge(budget: BudgetConfig): String = when (budget.frequency) {
        BudgetFrequency.Weekly -> "Semanal · ${DateLabels.weekdayName(budget.weekday)}"
        BudgetFrequency.Biweekly -> "Quincenal · ${budget.firstDay} y ${budget.secondDay}"
        BudgetFrequency.Monthly -> "Mensual · día ${budget.monthlyDay}"
        BudgetFrequency.Custom -> "Cada ${budget.customInterval} días"
    }

    /** Desglose por categoría computado de los movimientos (mayor a menor). */
    private fun breakdown(
        movements: List<Movement>,
        categories: Map<String, Category>,
    ): List<CategoryBreakdown> {
        if (movements.isEmpty()) return emptyList()
        val totalCents = movements.map { it.amount }.total().cents.coerceAtLeast(1)
        return movements
            .groupBy { it.categoryId }
            .map { (catId, list) ->
                val cat = categories[catId] ?: Category.byId(catId)
                val sum = list.map { it.amount }.total()
                Triple(cat, list.size, sum)
            }
            .sortedByDescending { (_, _, sum) -> sum.cents }
            .map { (cat, count, sum) ->
                CategoryBreakdown(
                    name = cat.name,
                    sub = "$count ${if (count == 1) "movimiento" else "movimientos"}",
                    amount = Calc.formatAmount(sum.major),
                    pct = sum.cents.toFloat() / totalCents,
                    tag = cat.tag,
                    tone = cat.tone,
                )
            }
    }
}
