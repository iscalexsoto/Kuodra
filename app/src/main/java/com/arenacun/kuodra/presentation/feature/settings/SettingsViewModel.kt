package com.arenacun.kuodra.presentation.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.BudgetFrequency
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcKey
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.repository.AuthRepository
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
 * Ajustes del espacio (pantalla adaptativa por caso de uso). Lee el [SettingsRepository] y
 * persiste el [SpaceSettings] completo tras cada edición; los overlays (calculadora de
 * monto, sheet de contacto) viven en el estado local.
 */
class SettingsViewModel(
    spaceRepository: SpaceRepository,
    private val settingsRepository: SettingsRepository,
    private val preferences: PreferencesRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {

    val useCase = spaceRepository.activeSpace.value.useCase

    /** Correo de la sesión activa (para mostrar en la sección de cuenta). */
    val accountEmail: String? = authRepository.session.value?.email

    /** Evento de una sola vez: la sesión se cerró ⇒ navegar al flujo de auth. */
    private val _signedOut = Channel<Unit>(Channel.BUFFERED)
    val signedOut = _signedOut.receiveAsFlow()

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
    )

    private val local = MutableStateFlow(Local())

    val uiState = combine(settingsRepository.settings(useCase), preferences.darkTheme, local) { settings, dark, l ->
        SettingsUiState(useCase, settings, dark, l.calcTarget, l.calc, l.editingContact)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(useCase))

    private fun current(): SpaceSettings = settingsRepository.settings(useCase).value
    private fun save(settings: SpaceSettings) = settingsRepository.update(useCase, settings)

    fun onNameChange(name: String) = save(current().copy(name = name))
    fun onToggleReminder() = save(current().copy(reminderEnabled = !current().reminderEnabled))
    fun onToggleTheme() = preferences.toggleTheme()

    // ---- Presupuesto (Personal) ----
    fun onToggleBudget() = current().budget?.let { save(current().copy(budget = it.copy(enabled = !it.enabled))) }
    fun onSetFrequency(f: BudgetFrequency) = current().budget?.let { save(current().copy(budget = it.copy(frequency = f))) }

    /** Día de la semana (Semanal): rota 1..7 con wraparound. */
    fun onWeekdayDelta(delta: Int) = updateBudget {
        it.copy(weekday = ((it.weekday - 1 + delta).mod(7)) + 1)
    }
    /** Quincenal: primer día de ingreso (1..28). */
    fun onFirstDayDelta(delta: Int) = updateBudget { it.copy(firstDay = (it.firstDay + delta).coerceIn(1, 28)) }
    /** Quincenal: segundo día de ingreso (1..31). */
    fun onSecondDayDelta(delta: Int) = updateBudget { it.copy(secondDay = (it.secondDay + delta).coerceIn(1, 31)) }
    /** Mensual: día del mes de ingreso (1..31). */
    fun onMonthlyDayDelta(delta: Int) = updateBudget { it.copy(monthlyDay = (it.monthlyDay + delta).coerceIn(1, 31)) }
    /** Personalizado: cada N días (2..90). */
    fun onCustomIntervalDelta(delta: Int) = updateBudget {
        it.copy(customInterval = (it.customInterval + delta).coerceIn(2, 90))
    }

    private inline fun updateBudget(transform: (com.arenacun.kuodra.domain.model.BudgetConfig) -> com.arenacun.kuodra.domain.model.BudgetConfig) {
        current().budget?.let { save(current().copy(budget = transform(it))) }
    }

    // ---- Calculadora de monto (presupuesto / fondo) ----
    fun onOpenCalc(target: CalcTarget) = local.update { it.copy(calcTarget = target, calc = CalcState()) }
    fun onCalcKey(key: CalcKey) = local.update { it.copy(calc = Calc.press(it.calc, key)) }
    fun onDismissCalc() = local.update { it.copy(calcTarget = null) }
    fun onConfirmCalc() {
        val l = local.value
        val result = l.calc.result
        if (result != null) {
            val amount = Calc.formatAmount(result)
            when (l.calcTarget) {
                CalcTarget.Budget -> current().budget?.let { save(current().copy(budget = it.copy(amount = amount))) }
                CalcTarget.Fund -> current().fund?.let { save(current().copy(fund = it.copy(initial = amount))) }
                null -> {}
            }
        }
        local.update { it.copy(calcTarget = null) }
    }

    // ---- Contactos / miembros ----
    fun onAddContact() = local.update { it.copy(editingContact = ContactDraft(null, "", "")) }
    fun onEditContact(person: Person) = local.update {
        it.copy(editingContact = ContactDraft(person.name, person.name, ""))
    }
    fun onContactName(v: String) = local.update { it.copy(editingContact = it.editingContact?.copy(name = v)) }
    fun onContactWhatsapp(v: String) = local.update { it.copy(editingContact = it.editingContact?.copy(whatsapp = v)) }
    fun onCloseContact() = local.update { it.copy(editingContact = null) }

    fun onSaveContact() {
        val draft = local.value.editingContact ?: return
        if (draft.name.isBlank()) return
        val s = current()
        val members = if (draft.original == null) {
            s.members + Person(draft.name, "Miembro", "", null, draft.name.take(1), AvatarTone.Tint)
        } else {
            s.members.map {
                if (it.name == draft.original) it.copy(name = draft.name, initials = draft.name.take(1)) else it
            }
        }
        save(s.copy(members = members))
        local.update { it.copy(editingContact = null) }
    }

    fun onDeleteContact() {
        val orig = local.value.editingContact?.original ?: return
        save(current().copy(members = current().members.filterNot { it.name == orig }))
        local.update { it.copy(editingContact = null) }
    }
}
