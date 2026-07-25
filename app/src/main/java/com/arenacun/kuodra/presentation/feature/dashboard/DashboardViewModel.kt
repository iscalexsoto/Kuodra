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
import com.arenacun.kuodra.domain.model.Settlement
import com.arenacun.kuodra.domain.model.SettlementKind
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.initialsOf
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.domain.model.total
import com.arenacun.kuodra.domain.usecase.SettleSuggestions
import com.arenacun.kuodra.domain.usecase.ShareSummary
import com.arenacun.kuodra.domain.usecase.SharedBalances
import com.arenacun.kuodra.domain.repository.MovementRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SettlementRepository
import com.arenacun.kuodra.domain.repository.SnapshotRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import com.arenacun.kuodra.domain.usecase.BudgetPeriod
import com.arenacun.kuodra.domain.usecase.ClosePeriod
import com.arenacun.kuodra.presentation.feature.movement.toUi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val settlementRepository: SettlementRepository,
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
                settlementRepository.settlements(space.id),
            ) { movements, settings, people, settlements ->
                val categories = summaryRepository.categories().associateBy { it.id }
                val persons = people.associate { it.id to it.name }
                val gastos = space.useCase == UseCase.Gastos
                val payments = livePayments(settlements)
                DashboardUiState(
                    space = space,
                    movements = movements.map { it.toUi(categories, space.useCase, today, persons) },
                    people = if (gastos) peopleBalances(movements, payments, people) else emptyList(),
                    categories = breakdown(movements, categories),
                    personalHero = if (space.useCase == UseCase.Personal) personalHero(movements, settings) else null,
                    gastosHero = if (gastos) gastosHero(movements, payments) else null,
                    membersLabel = if (gastos) membersLabel(people) else null,
                    hasUnsettledBalances = gastos && SharedBalances.compute(movements, payments).isNotEmpty(),
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

    // ---- Compartir resumen (Gastos) ----
    /** Texto de resumen listo para el share nativo (lo construye el VM; lo lanza la pantalla). */
    private val _share = Channel<String>(Channel.BUFFERED)
    val share = _share.receiveAsFlow()

    /** Arma el resumen del grupo con los saldos vivos y lo emite para compartir; cierra el menú. */
    fun onShare() {
        viewModelScope.launch {
            val space = spaceRepository.activeSpace.value
            val movements = movementRepository.movements(space.id).first()
            val contacts = personRepository.persons(space.id).first().associateBy { it.id }
            val payments = livePayments(settlementRepository.settlements(space.id).first())
            _share.send(ShareSummary.build(space.displayName, movements, contacts, payments))
            menu.update { it.copy(sheet = DashboardSheet.None) }
        }
    }

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

    /**
     * Personas del dashboard Gastos con su saldo (desde los movimientos vivos). Neto de una persona
     * < 0 ⇒ debe al grupo ("te debe" desde tu vista); > 0 ⇒ el grupo le debe ("le debes").
     */
    private fun peopleBalances(
        movements: List<Movement>,
        payments: List<Settlement>,
        contacts: List<SpacePerson>,
    ): List<Person> {
        val balances = SharedBalances.compute(movements, payments)
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

    /** Pagos individuales vivos (kind=Payment, no consumidos por un corte). */
    private fun livePayments(settlements: List<Settlement>): List<Settlement> =
        settlements.filter { it.kind == SettlementKind.Payment && it.settledBy.isEmpty() }

    /** Hero Gastos: tu saldo neto + totales que te deben / debes (desde las transferencias sugeridas). */
    private fun gastosHero(movements: List<Movement>, payments: List<Settlement>): GastosHero {
        val balances = SharedBalances.compute(movements, payments)
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

    /** Subtítulo de miembros del encabezado (Gastos): "Tú" + contactos. */
    private fun membersLabel(contacts: List<SpacePerson>): String {
        val count = contacts.size + 1 // incluye "Tú"
        return if (count <= 1) "Solo tú" else "$count miembros"
    }

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
