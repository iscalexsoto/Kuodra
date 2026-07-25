package com.arenacun.kuodra.presentation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import com.arenacun.kuodra.domain.model.Countries
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcKey
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.Session
import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.SpacePerson
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.SplitMode
import com.arenacun.kuodra.domain.model.SplitRule
import com.arenacun.kuodra.domain.model.SplitRuleShare
import com.arenacun.kuodra.domain.model.ThemeMode
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.initialsOf
import com.arenacun.kuodra.domain.model.newId
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.domain.usecase.SplitRuleCalc
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.repository.PersonRepository
import com.arenacun.kuodra.domain.repository.PreferencesRepository
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Ajustes del espacio (pantalla adaptativa por caso de uso). Personal usa el [SettingsRepository]
 * (presupuesto); Gastos usa [SpaceRepository] (nombre, recordatorio, archivar) y [PersonRepository]
 * (contactos con teléfono). Los overlays (calculadora, sheet de contacto) viven en el estado local.
 */
class SettingsViewModel(
    private val spaceRepository: SpaceRepository,
    private val settingsRepository: SettingsRepository,
    private val personRepository: PersonRepository,
    private val preferences: PreferencesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val activeSpace: Space = spaceRepository.activeSpace.value
    val useCase = activeSpace.useCase
    private val spaceId = activeSpace.id

    /** Correo de la sesión activa (para mostrar en la sección de cuenta). */
    val accountEmail: String? = authRepository.session.value?.email

    /** Evento de una sola vez: la sesión se cerró ⇒ navegar al flujo de auth. */
    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut = _signedOut.receiveAsFlow()

    /** Evento de una sola vez: el espacio se archivó ⇒ volver al dashboard (ya en Personal). */
    private val _closed = Channel<Unit>(Channel.BUFFERED)
    val closed = _closed.receiveAsFlow()

    fun onSignOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _signedOut.send(Unit)
        }
    }

    private data class Local(
        val calcTarget: CalcTarget? = null,
        val calc: CalcState = CalcState(),
        val editingContact: ContactDraft? = null,
        val editingName: String? = null,
        /** Copia de trabajo del presupuesto (Personal): edición síncrona sin esperar a Room. */
        val budgetEdit: BudgetConfig? = null,
        /**
         * Copia de trabajo de la regla de división (Gastos), por la misma razón que [budgetEdit]. No
         * se limpia cuando el valor persistido la alcanza; vive lo que dura la pantalla.
         */
        val ruleEdit: SplitRule? = null,
        val rulePadPersonId: String? = null,
        val rulePad: CalcState = CalcState(),
    )

    private val local = MutableStateFlow(Local())

    private data class Base(
        val personal: SpaceSettings,
        val space: Space,
        val persons: List<SpacePerson>,
        val theme: ThemeMode,
        val session: Session?,
    )

    private val base = combine(
        settingsRepository.settings(),
        spaceRepository.activeSpace,
        personRepository.persons(spaceId),
        preferences.themeMode,
        authRepository.session,
    ) { personal, space, persons, theme, session -> Base(personal, space, persons, theme, session) }

    val uiState = combine(base, local) { b, l ->
        val rule = l.ruleEdit ?: b.space.splitRule
        val settings = if (useCase == UseCase.Personal) {
            if (l.budgetEdit != null) b.personal.copy(budget = l.budgetEdit) else b.personal
        } else {
            SpaceSettings(
                name = b.space.displayName,
                members = b.persons.map { it.toDisplay() },
                budget = null,
                reminderEnabled = b.space.reminderEnabled,
                splitRule = rule,
            )
        }
        SettingsUiState(
            useCase = useCase,
            settings = settings,
            contacts = b.persons,
            themeMode = b.theme,
            calcTarget = l.calcTarget,
            calc = l.calc,
            editingContact = l.editingContact,
            accountName = b.session?.name.orEmpty(),
            editingName = l.editingName,
            rulePadPersonId = l.rulePadPersonId,
            rulePad = l.rulePad,
            splitRuleError = if (useCase == UseCase.Gastos && rule.enabled)
                SplitRuleCalc.validate(rule, memberIdsOf(b.persons)) else null,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(useCase))

    private fun memberIdsOf(persons: List<SpacePerson>): List<String> =
        listOf(PersonRef.ME) + persons.map { it.id }

    /** Contactos vigentes: sembrar la regla no puede depender de que la pantalla esté suscrita. */
    private var currentPersons: List<SpacePerson> = emptyList()

    init {
        if (useCase == UseCase.Gastos) viewModelScope.launch {
            personRepository.persons(spaceId).collect { currentPersons = it }
        }
    }

    private fun SpacePerson.toDisplay(): Person =
        Person(name, phone.ifBlank { "Sin WhatsApp" }, "", null, initialsOf(name), toneForName(name))

    // ---- Nombre / recordatorio (Gastos) ----
    fun onNameChange(name: String) {
        if (useCase == UseCase.Gastos) viewModelScope.launch { spaceRepository.rename(spaceId, name) }
    }
    fun onToggleReminder() {
        if (useCase == UseCase.Gastos) viewModelScope.launch {
            spaceRepository.setReminder(spaceId, !spaceRepository.activeSpace.value.reminderEnabled)
        }
    }
    fun onArchiveSpace() {
        if (useCase != UseCase.Gastos) return
        viewModelScope.launch {
            spaceRepository.archive(spaceId)
            _closed.send(Unit)
        }
    }

    // ---- División por defecto (Gastos) ----
    /**
     * Al encender la regla por primera vez se siembra con todos los miembros a partes iguales, para
     * que nunca quede en un estado vacío que no se pueda aplicar; apagarla conserva la configuración.
     */
    fun onToggleSplitRule() = editRule { rule ->
        val enabling = !rule.enabled
        val shares = if (enabling && rule.shares.isEmpty())
            SplitRuleCalc.evenPercents(memberIdsOf(currentPersons)) else rule.shares
        rule.copy(enabled = enabling, shares = shares)
    }

    fun onSetRuleMode(mode: SplitMode) = editRule { rule ->
        val shares = if (mode == SplitMode.Percent && rule.shares.sumOf { it.percent } <= 0)
            SplitRuleCalc.evenPercents(rule.participantIds) else rule.shares
        rule.copy(mode = mode, shares = shares)
    }

    /**
     * Añade/quita un participante. En modo Percent los porcentajes anteriores dejan de sumar 100, así
     * que se reparten parejo como punto de partida válido (el usuario los ajusta con el number pad).
     */
    fun onToggleRuleParticipant(id: String) = editRule { rule ->
        val present = rule.shares.any { it.personId == id }
        val ids = rule.participantIds.toSet().let { if (present) it - id else it + id }
        // Orden canónico (el mismo que ve el usuario y que usa el alta).
        val ordered = memberIdsOf(currentPersons).filter { it in ids }
        rule.copy(
            shares = if (rule.mode == SplitMode.Percent) SplitRuleCalc.evenPercents(ordered)
            else ordered.map { SplitRuleShare(it, rule.percentOf(it)) },
        )
    }

    fun onDistributeRuleEvenly() = editRule { it.copy(shares = SplitRuleCalc.evenPercents(it.participantIds)) }

    fun onSetRulePayer(id: String) = editRule { it.copy(payerId = id) }

    fun onToggleRuleAutoPersonalCopy() = editRule { it.copy(autoPersonalCopy = !it.autoPersonalCopy) }

    /**
     * Edita la regla sobre una copia de trabajo síncrona (igual que [editBudget]) y persiste. La regla
     * puede quedar guardada incompleta (% que no suman 100) mientras el usuario teclea; `SplitRuleCalc`
     * la sanea al aplicarla, y la pantalla muestra el aviso en vivo.
     */
    private fun editRule(transform: (SplitRule) -> SplitRule) {
        if (useCase != UseCase.Gastos) return
        val base = local.value.ruleEdit ?: spaceRepository.activeSpace.value.splitRule
        val updated = transform(base)
        local.update { it.copy(ruleEdit = updated) }
        viewModelScope.launch { spaceRepository.setSplitRule(spaceId, updated) }
    }

    // ---- Number pad del % de un participante de la regla ----
    fun onOpenRulePad(personId: String) = local.update {
        val rule = it.ruleEdit ?: spaceRepository.activeSpace.value.splitRule
        it.copy(rulePadPersonId = personId, rulePad = Calc.initial(rule.percentOf(personId).toDouble()))
    }
    fun onRulePadKey(key: CalcKey) = local.update { it.copy(rulePad = Calc.press(it.rulePad, key)) }
    fun onDismissRulePad() = local.update { it.copy(rulePadPersonId = null) }
    fun onConfirmRulePad() {
        val l = local.value
        val id = l.rulePadPersonId
        val percent = l.rulePad.result?.toInt()?.coerceIn(0, 100)
        if (id != null && percent != null) {
            editRule { rule ->
                rule.copy(shares = rule.shares.map { if (it.personId == id) it.copy(percent = percent) else it })
            }
        }
        local.update { it.copy(rulePadPersonId = null) }
    }

    fun onSetThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    // ---- Presupuesto (Personal) ----
    fun onToggleBudget() = editBudget { it.copy(enabled = !it.enabled) }
    fun onSetFrequency(f: BudgetFrequency) = editBudget { it.copy(frequency = f) }
    fun onWeekdayDelta(delta: Int) = editBudget { it.copy(weekday = ((it.weekday - 1 + delta).mod(7)) + 1) }
    fun onFirstDayDelta(delta: Int) = editBudget { it.copy(firstDay = (it.firstDay + delta).coerceIn(1, 28)) }
    fun onSecondDayDelta(delta: Int) = editBudget { it.copy(secondDay = (it.secondDay + delta).coerceIn(1, 31)) }
    fun onMonthlyDayDelta(delta: Int) = editBudget { it.copy(monthlyDay = (it.monthlyDay + delta).coerceIn(1, 31)) }
    fun onCustomIntervalDelta(delta: Int) = editBudget {
        it.copy(customInterval = (it.customInterval + delta).coerceIn(2, 90))
    }
    /**
     * Edita el presupuesto sobre una copia de trabajo síncrona (evita perder taps rápidos por la
     * latencia de Room) y persiste el cambio.
     */
    private fun editBudget(transform: (BudgetConfig) -> BudgetConfig) {
        val base = local.value.budgetEdit ?: settingsRepository.settings().value.budget ?: return
        val updated = transform(base)
        local.update { it.copy(budgetEdit = updated) }
        settingsRepository.updateBudget(updated)
    }

    // ---- Calculadora de monto (presupuesto) ----
    fun onOpenCalc(target: CalcTarget) = local.update {
        val text = when (target) {
            CalcTarget.Budget -> it.budgetEdit?.amount ?: settingsRepository.settings().value.budget?.amount
        }
        it.copy(calcTarget = target, calc = Calc.initial(text?.let(Calc::parseAmount)))
    }
    fun onCalcKey(key: CalcKey) = local.update { it.copy(calc = Calc.press(it.calc, key)) }
    fun onDismissCalc() = local.update { it.copy(calcTarget = null) }
    fun onConfirmCalc() {
        val l = local.value
        val result = l.calc.result
        if (result != null) {
            val amount = Calc.formatAmount(result)
            when (l.calcTarget) {
                CalcTarget.Budget -> editBudget { it.copy(amount = amount) }
                null -> {}
            }
        }
        local.update { it.copy(calcTarget = null) }
    }

    // ---- Contactos (Gastos) ----
    fun onAddContact() = local.update { it.copy(editingContact = ContactDraft(null, "", "")) }
    fun onEditContact(person: SpacePerson) = local.update {
        // Separa el teléfono guardado en (código de país, número local) para pre-poblar el selector.
        val (dialCode, localNumber) = Countries.split(person.phone)
        it.copy(editingContact = ContactDraft(person.id, person.name, localNumber, dialCode))
    }
    fun onContactName(v: String) = local.update { it.copy(editingContact = it.editingContact?.copy(name = v)) }
    fun onContactWhatsapp(v: String) = local.update {
        // Solo dígitos en el número local (el código va aparte).
        it.copy(editingContact = it.editingContact?.copy(whatsapp = v.filter { ch -> ch.isDigit() }))
    }
    fun onOpenCountryPicker() = local.update { it.copy(editingContact = it.editingContact?.copy(countryPickerOpen = true)) }
    fun onPickCountry(dialCode: String) = local.update {
        it.copy(editingContact = it.editingContact?.copy(dialCode = dialCode, countryPickerOpen = false))
    }
    fun onCloseCountryPicker() = local.update { it.copy(editingContact = it.editingContact?.copy(countryPickerOpen = false)) }
    fun onCloseContact() = local.update { it.copy(editingContact = null) }

    fun onSaveContact() {
        val draft = local.value.editingContact ?: return
        if (draft.name.isBlank()) return
        val localNumber = draft.whatsapp.filter { it.isDigit() }
        val person = SpacePerson(
            id = draft.id ?: newId(),
            name = draft.name.trim(),
            // Guardado con el código de país incluido (formato que espera `wa.me`).
            phone = if (localNumber.isBlank()) "" else draft.dialCode + localNumber,
        )
        viewModelScope.launch {
            if (draft.id == null) personRepository.add(spaceId, person)
            else personRepository.update(spaceId, person)
        }
        local.update { it.copy(editingContact = null) }
    }

    fun onDeleteContact() {
        val id = local.value.editingContact?.id ?: return
        viewModelScope.launch { personRepository.delete(id) }
        local.update { it.copy(editingContact = null) }
    }

    // ---- Nombre de usuario (sección Cuenta) ----
    fun onEditName() = local.update { it.copy(editingName = authRepository.session.value?.name.orEmpty()) }
    fun onNameDraftChange(value: String) = local.update { it.copy(editingName = value) }
    fun onCloseName() = local.update { it.copy(editingName = null) }

    fun onConfirmName() {
        val name = local.value.editingName?.trim().orEmpty()
        if (name.isBlank()) return
        viewModelScope.launch { authRepository.updateName(name) }
        local.update { it.copy(editingName = null) }
    }
}
