package com.arenacun.kuodra.presentation.feature.settle

import com.arenacun.kuodra.domain.model.CalcState
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.UseCase

data class SettleUiState(
    val title: String = "",
    val useCase: UseCase = UseCase.Gastos,
    val people: List<SettlePersonRow> = emptyList(),
    val heroLabel: String = "",
    val heroAmount: String = "",
    /** Solo Gastos: total que te deben. */
    val owedAmount: String? = null,
    /** Solo Gastos: total que debes. */
    val oweAmount: String? = null,
    val confirmLabel: String = "",
    /** Hay saldos vivos que liquidar: habilita el botón "Registrar liquidación". */
    val canRegister: Boolean = false,
    /** true = mostrando la confirmación de la liquidación total. */
    val showRegisterConfirm: Boolean = false,
    /** Persona cuyo pago se está capturando en el number pad (null = cerrado). */
    val payPadPersonId: String? = null,
    /** Estado de trabajo del number pad del pago. */
    val payPad: CalcState = CalcState(),
)

/** Fila de "quién debe a quién" con lo necesario para WhatsApp y para liquidar por persona. */
data class SettlePersonRow(
    val personId: String,
    val person: Person,
    /** true si el contacto tiene teléfono (habilita WhatsApp). */
    val hasPhone: Boolean,
    /** Saldo neto de la persona en centavos (para precargar el pad y validar). */
    val netCents: Long = 0,
)
