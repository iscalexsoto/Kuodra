package com.arenacun.kuodra.presentation.feature.replenish

import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.CalcState

data class ReplenishUiState(
    val fundName: String = "",
    val current: String = "$0",
    val initial: String = "$0",
    val suggested: String = "$0",
    val amount: Double? = null,
    val calc: CalcState = CalcState(),
    val showCalculator: Boolean = false,
    val note: String = "",
) {
    val amountLabel: String get() = amount?.let { Calc.formatAmount(it) } ?: "$0"
    val hasAmount: Boolean get() = amount != null
}
