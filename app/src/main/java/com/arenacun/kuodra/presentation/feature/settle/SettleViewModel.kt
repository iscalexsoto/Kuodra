package com.arenacun.kuodra.presentation.feature.settle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.repository.SpaceRepository
import com.arenacun.kuodra.domain.repository.SummaryRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * Liquidación (Gastos) / corte de caja (Caja) — `scrSettle`. Calcula los totales a partir de
 * las personas del [SummaryRepository] y emite [done] al registrar para volver al dashboard.
 */
class SettleViewModel(
    spaceRepository: SpaceRepository,
    summaryRepository: SummaryRepository,
) : ViewModel() {

    private val space = spaceRepository.activeSpace.value
    private val useCase = space.useCase
    private val people = summaryRepository.people(useCase)

    private val _uiState = MutableStateFlow(buildState())
    val uiState = _uiState.asStateFlow()

    private val _done = Channel<Unit>(Channel.BUFFERED)
    val done = _done.receiveAsFlow()

    fun onRegister() = viewModelScope.launch { _done.send(Unit) }

    private fun buildState(): SettleUiState {
        val t = space.terminology
        return when (useCase) {
            UseCase.Gastos -> {
                val owed = people.filter { it.positive == true }.sumOf { amountOf(it.amount) }
                val owe = people.filter { it.positive == false }.sumOf { -amountOf(it.amount) }
                SettleUiState(
                    title = t.settleTitle.ifBlank { "Liquidación" },
                    useCase = useCase,
                    people = people,
                    heroLabel = "Saldo neto a tu favor",
                    heroAmount = Calc.formatAmount(owed - owe),
                    owedAmount = Calc.formatAmount(owed),
                    oweAmount = Calc.formatAmount(owe),
                    confirmLabel = "Registrar liquidación",
                )
            }
            UseCase.Caja -> {
                val total = people.sumOf { amountOf(it.amount) }
                SettleUiState(
                    title = t.settleTitle.ifBlank { "Corte de caja" },
                    useCase = useCase,
                    people = people,
                    heroLabel = "Movimientos del periodo",
                    heroAmount = Calc.formatAmount(total),
                    confirmLabel = "Registrar corte",
                )
            }
            UseCase.Personal -> SettleUiState(useCase = useCase)
        }
    }

    /** Convierte "+$450" / "−$200" / "$1,700" a Double con signo. */
    private fun amountOf(text: String): Double {
        val negative = text.contains('−') || text.contains('-')
        val digits = text.filter { it.isDigit() || it == '.' }
        val value = digits.toDoubleOrNull() ?: 0.0
        return if (negative) -value else value
    }
}
