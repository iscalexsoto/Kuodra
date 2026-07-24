package com.arenacun.kuodra.presentation.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.ReturnStatus
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.initialsOf
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.domain.model.total
import com.arenacun.kuodra.domain.usecase.SettleSuggestions
import com.arenacun.kuodra.domain.usecase.SharedBalances
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SnapshotRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import com.arenacun.kuodra.domain.usecase.BudgetPeriod
import com.arenacun.kuodra.domain.usecase.ClosePeriod
import com.arenacun.kuodra.domain.usecase.ReturnCalc
import com.arenacun.kuodra.presentation.feature.movement.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val spaceRepository: SpaceRepository,
    private val movementRepository: MovementRepository,
    private val summaryRepository: SummaryRepository,
    private val settingsRepository: SettingsRepository,
    private val snapshotRepository: SnapshotRepository,
    private val personRepository: PersonRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()

    /** Espacios de Gastos (para el selector). */
    val spaces = spaceRepository.spaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState = spaceRepository.activeSpace
        .flatMapLatest { space ->
            combine(
                movementRepository.movements(space.id),
                settingsRepository.settings(),
                personRepository.persons(space.id),
            ) { movements, settings, people ->
                val categories = summaryRepository.categories().associateBy { it.id }
                val percent = settings.budget?.returnPercent ?: ReturnCalc.DEFAULT_RETURN_PERCENT
                val persons = people.associate { it.id to it.name }
                val gastos = space.useCase == UseCase.Gastos
                DashboardUiState(
                    space = space,
                    movements = movements.map { it.toUi(categories, space.useCase, today, percent, persons) },
                    people = if (gastos) peopleBalances(movements, people) else emptyList(),
                    categories = breakdown(movements, categories),
                    personalHero = if (space.useCase == UseCase.Personal) personalHero(movements, settings) else null,
                    gastosHero = if (gastos) gastosHero(movements) else null,
                    pendingReturns = if (space.useCase == UseCase.Personal) pendingReturns(movements, percent) else null,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    private val menu = MutableStateFlow(DashboardOverlay())

    val overlay = menu.asStateFlow()

    // ---- Hojas inferiores (espacios / crear / menú) ----
    fun onOpenSpaces() = menu.update { it.copy(sheet = DashboardSheet.Spaces) }
    fun onOpenMenu() = menu.update { it.copy(sheet = DashboardSheet.Menu) }
    fun onOpenAddOptions() = menu.update { it.copy(sheet = DashboardSheet.AddOptions) }
    fun onCloseSheet() = menu.update { it.copy(sheet = DashboardSheet.None) }

    // ---- Compartir resumen/corte (Gastos/Caja) ----
    fun onShare() = menu.update { it.copy(sheet = DashboardSheet.Share) }
    fun onShareConfirm() = menu.update { it.copy(sheet = DashboardSheet.Shared) }

    // ---- Cerrar periodo (Personal) ----
    fun onClosePeriod() = menu.update { it.copy(sheet = DashboardSheet.PCloseConfirm) }

    /** Congela el periodo actual en un snapshot (historial) y muestra la confirmación. */
    fun onClosePeriodConfirm() {
        viewModelScope.launch {
            val useCase = spaceRepository.activeSpace.value.useCase
            if (useCase == UseCase.Personal) {
                val movements = movementRepository.movements("").first()
                val budget = settingsRepository.settings().value.budget
                val categories = summaryRepository.categories().associateBy { it.id }
                snapshotRepository.add(ClosePeriod.build(budget, movements, categories, today))
            }
            menu.update { it.copy(sheet = DashboardSheet.PClosed) }
        }
    }

    // ---- Marcar todo como devuelto (Personal) ----
    fun onMarkAllReturned() = menu.update { it.copy(sheet = DashboardSheet.ReturnAllConfirm) }

    /** Estampa el % global vigente en cada movimiento por devolver y los pasa a Devuelto. */
    fun onMarkAllReturnedConfirm() {
        viewModelScope.launch {
            val useCase = spaceRepository.activeSpace.value.useCase
            if (useCase == UseCase.Personal) {
                val percent = settingsRepository.settings().value.budget?.returnPercent
                    ?: ReturnCalc.DEFAULT_RETURN_PERCENT
                val movements = movementRepository.movements("").first()
                movements.filter { it.returnStatus == ReturnStatus.Pending }.forEach { m ->
                    movementRepository.update(m.copy(returnStatus = ReturnStatus.Returned, returnPercent = percent))
                }
            }
            menu.update { it.copy(sheet = DashboardSheet.ReturnAllDone) }
        }
    }

    /**
     * Personas del dashboard Gastos con su saldo (desde los movimientos vivos). Neto de una persona
     * < 0 ⇒ debe al grupo ("te debe" desde tu vista); > 0 ⇒ el grupo le debe ("le debes").
     */
    private fun peopleBalances(movements: List<Movement>, contacts: List<SpacePerson>): List<Person> {
        val balances = SharedBalances.compute(movements)
        val byId = contacts.associateBy { it.id }
        return balances.filterKeys { it != PersonRef.ME }
            .entries.sortedByDescending { -it.value } // más deudores primero
            .map { (id, net) ->
                val name = byId[id]?.name ?: "(eliminado)"
                val owes = net < 0L
                Person(
                    name = name,
                    sub = if (owes) "te debe" else "le debes",
                    amount = (if (owes) "+" else "−") + Calc.formatAmount(kotlin.math.abs(net) / 100.0),
                    positive = owes,
                    initials = initialsOf(name),
                    tone = if (owes) AvatarTone.Pos else AvatarTone.Neg,
                )
            }
    }

    /** Hero Gastos: tu saldo neto + totales que te deben / debes (desde las transferencias sugeridas). */
    private fun gastosHero(movements: List<Movement>): GastosHero {
        val balances = SharedBalances.compute(movements)
        val transfers = SettleSuggestions.compute(balances)
        val owed = transfers.filter { it.toId == PersonRef.ME }.sumOf { it.amount.cents }
        val owe = transfers.filter { it.fromId == PersonRef.ME }.sumOf { it.amount.cents }
        val net = owed - owe
        return GastosHero(
            netLabel = (if (net >= 0) "+" else "−") + Calc.formatAmount(kotlin.math.abs(net) / 100.0),
            owedLabel = Calc.formatAmount(owed / 100.0),
            oweLabel = Calc.formatAmount(owe / 100.0),
            positive = net >= 0,
        )
    }

    /** Total "por cobrar": suma del reembolso vivo de TODOS los pendientes (sin ventana de periodo). */
    private fun pendingReturns(movements: List<Movement>, percent: Int): PendingReturnsUi? {
        val pending = movements.filter { it.returnStatus == ReturnStatus.Pending }
        if (pending.isEmpty()) return null
        val total = ReturnCalc.pendingTotal(pending, percent)
        return PendingReturnsUi(
            totalLabel = Calc.formatAmount(total.major),
            caption = "${pending.size} ${if (pending.size == 1) "movimiento" else "movimientos"} por devolver · $percent%",
        )
    }

    /** Activa el espacio Personal y cierra el selector. */
    fun onSelectPersonal() {
        spaceRepository.selectPersonal()
        menu.update { it.copy(sheet = DashboardSheet.None) }
    }

    /** Activa un espacio de Gastos por id y cierra el selector. */
    fun onSelectSpace(id: String) {
        spaceRepository.selectSpace(id)
        menu.update { it.copy(sheet = DashboardSheet.None) }
    }

    /** Restaura un espacio archivado. */
    fun onUnarchiveSpace(id: String) {
        viewModelScope.launch { spaceRepository.unarchive(id) }
    }

    // ---- Salir / archivar grupo ----
    fun onLeaveStart() = menu.update { it.copy(sheet = DashboardSheet.None, leaveStep = LeaveStep.Settle) }
    fun onLeaveAdvance() {
        val step = menu.value.leaveStep
        if (step == LeaveStep.Confirm) {
            // Confirmar archiva el espacio activo (vuelve a Personal por construcción).
            val id = spaceRepository.activeSpace.value.id
            if (id.isNotEmpty()) viewModelScope.launch { spaceRepository.archive(id) }
        }
        menu.update {
            it.copy(leaveStep = when (it.leaveStep) {
                LeaveStep.Settle -> LeaveStep.Confirm
                LeaveStep.Confirm -> LeaveStep.Done
                else -> it.leaveStep
            })
        }
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
