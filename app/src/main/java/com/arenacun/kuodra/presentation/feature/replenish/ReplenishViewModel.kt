package com.arenacun.kuodra.presentation.feature.replenish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcKey
import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SettingsRepository
import com.arenacun.kuodra.domain.repository.SpaceRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Reponer fondo de caja chica (`scrRepon`). Toma el monto inicial de los ajustes, sugiere lo
 * que falta para llenar el fondo y captura el monto con la calculadora. Emite [done] al registrar.
 */
class ReplenishViewModel(
    spaceRepository: SpaceRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val space = spaceRepository.activeSpace.value
    private val initial = settingsRepository.settings(UseCase.Caja).value.fund?.initial ?: "$5,000"
    private val current = "$900" // saldo actual (mock, igual al banner del dashboard)

    private val _uiState = MutableStateFlow(
        ReplenishUiState(
            fundName = space.displayName,
            current = current,
            initial = initial,
            suggested = Calc.formatAmount((amountOf(initial) - amountOf(current)).coerceAtLeast(0.0)),
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    fun onNoteChange(v: String) = _uiState.update { it.copy(note = v) }

    fun onOpenCalculator() = _uiState.update { it.copy(showCalculator = true, calc = CalcState()) }
    fun onCalcKey(key: CalcKey) = _uiState.update { it.copy(calc = Calc.press(it.calc, key)) }
    fun onDismissCalculator() = _uiState.update { it.copy(showCalculator = false) }
    fun onConfirmAmount() = _uiState.update { it.copy(amount = it.calc.result ?: it.amount, showCalculator = false) }

    fun onUseSuggested() = _uiState.update { it.copy(amount = amountOf(it.suggested)) }

    fun onRegister() = viewModelScope.launch { _done.send(Unit) }

    private fun amountOf(text: String): Double =
        text.filter { it.isDigit() || it == '.' }.toDoubleOrNull() ?: 0.0
}
