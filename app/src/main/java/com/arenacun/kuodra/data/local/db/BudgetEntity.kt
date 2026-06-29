package com.arenacun.kuodra.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Presupuesto del usuario (una fila por `owner`, que es la clave). Monto en centavos. Mismas
 * columnas de sincronización que el resto.
 */
@Entity(tableName = "budget")
data class BudgetEntity(
    @PrimaryKey val owner: String,
    val enabled: Boolean,
    val frequency: String,
    val amountCents: Long,
    val weekday: Int,
    val firstDay: Int,
    val secondDay: Int,
    val monthlyDay: Int,
    val customInterval: Int,
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    val remoteUpdated: String = "",
)
