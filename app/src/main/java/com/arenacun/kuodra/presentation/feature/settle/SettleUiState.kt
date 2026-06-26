package com.arenacun.kuodra.presentation.feature.settle

import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.UseCase

data class SettleUiState(
    val title: String = "",
    val useCase: UseCase = UseCase.Gastos,
    val people: List<Person> = emptyList(),
    val heroLabel: String = "",
    val heroAmount: String = "",
    /** Solo Gastos: total que te deben. */
    val owedAmount: String? = null,
    /** Solo Gastos: total que debes. */
    val oweAmount: String? = null,
    val confirmLabel: String = "",
)
