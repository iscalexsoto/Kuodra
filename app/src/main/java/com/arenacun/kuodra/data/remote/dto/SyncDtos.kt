package com.arenacun.kuodra.data.remote.dto

import kotlinx.serialization.Serializable

/** Respuesta paginada estándar de la API de records de PocketBase. */
@Serializable
data class PbListResponse<T>(
    val page: Int = 1,
    val perPage: Int = 0,
    val totalItems: Int = 0,
    val totalPages: Int = 0,
    val items: List<T> = emptyList(),
)

/**
 * Registro de movimiento en PocketBase. `updated` es el timestamp de sistema (cursor de deltas +
 * last-write-wins). `date` se guarda como texto ISO (yyyy-MM-dd) y `splitNames` como campo json.
 */
@Serializable
data class MovementDto(
    val id: String,
    val owner: String = "",
    val amount: Long = 0,
    val category: String = "",
    val title: String = "",
    val note: String = "",
    val date: String = "",
    val payer: String? = null,
    val splitNames: List<String> = emptyList(),
    val deleted: Boolean = false,
    val updated: String = "",
)

@Serializable
data class CategoryDto(
    val id: String,
    val owner: String = "",
    val name: String = "",
    val tag: String = "",
    val tone: String = "",
    val archived: Boolean = false,
    val deleted: Boolean = false,
    val updated: String = "",
)

@Serializable
data class BudgetDto(
    val id: String,
    val owner: String = "",
    val enabled: Boolean = false,
    val frequency: String = "Biweekly",
    val amount: Long = 0,
    val weekday: Int = 1,
    val firstDay: Int = 1,
    val secondDay: Int = 16,
    val monthlyDay: Int = 1,
    val customInterval: Int = 15,
    val deleted: Boolean = false,
    val updated: String = "",
)

@Serializable
data class PeriodLineDto(
    val categoryName: String = "",
    val count: Int = 0,
    val amount: Long = 0,
    val tone: String = "Tint",
)

@Serializable
data class PeriodSnapshotDto(
    val id: String,
    val owner: String = "",
    val title: String = "",
    val periodStart: String = "",
    val periodEnd: String = "",
    val totalSpent: Long = 0,
    val budgetAmount: Long? = null,
    val lines: List<PeriodLineDto> = emptyList(),
    val createdAt: Long = 0,
    val deleted: Boolean = false,
    val updated: String = "",
)
