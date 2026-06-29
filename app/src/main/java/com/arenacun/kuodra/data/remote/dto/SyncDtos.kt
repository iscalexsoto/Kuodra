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
