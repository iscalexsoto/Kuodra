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
    /** Espacio de Gastos; vacío = Personal. */
    val space: String = "",
    /** Pagadores del gasto (Gastos). */
    val payers: List<PayerShareDto> = emptyList(),
    /** Modo de división elegido en la captura (`None`/`Equal`/`Amount`/`Percent`). */
    val splitMode: String = "",
    /** División resuelta a centavos (Gastos). */
    val splits: List<SplitShareDto> = emptyList(),
    /** Id del corte que liquidó este gasto; vacío = vivo. */
    val settlementId: String = "",
    val items: List<MovementItemDto> = emptyList(),
    /** Raw OCR del ticket (vacío ⇔ sin escaneo; PocketBase devuelve "" en text vacíos). */
    val scanRawText: String = "",
    /** Nombre del enum `ScanSource` (`Camera`/`Gallery`) o vacío. */
    val scanSource: String = "",
    /** Nombre del enum `ReturnStatus` (`None`/`Pending`/`Returned`) o vacío ⇔ None. */
    val returnStatus: String = "",
    /** % de devolución congelado (Returned); 0 ⇔ sin estampar. */
    val returnPercent: Int = 0,
    val deleted: Boolean = false,
    val updated: String = "",
)

/** Partida del desglose de un movimiento. `amount` en centavos. Campo json en PocketBase. */
@Serializable
data class MovementItemDto(
    val id: String,
    val concept: String = "",
    val amount: Long = 0,
    val payer: String? = null,
    /** Personal: si la partida entra en la devolución. Legacy sin la clave ⇒ true. */
    val returnable: Boolean = true,
)

/** Pagador de un gasto compartido (referencia por id). `amount` en centavos. Campo json. */
@Serializable
data class PayerShareDto(
    val personId: String = "",
    val amount: Long = 0,
)

/** Parte resuelta de un participante de la división. `share` en centavos. Campo json. */
@Serializable
data class SplitShareDto(
    val personId: String = "",
    val share: Long = 0,
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
    /** % global a devolver de los movimientos "Por devolver" (5..100). */
    val returnPercent: Int = 75,
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

/** Espacio de Gastos compartidos. */
@Serializable
data class SpaceDto(
    val id: String,
    val owner: String = "",
    val name: String = "",
    val archived: Boolean = false,
    val reminderEnabled: Boolean = true,
    val deleted: Boolean = false,
    val updated: String = "",
)

/** Contacto de un espacio (Nombre + Teléfono). */
@Serializable
data class PersonDto(
    val id: String,
    val owner: String = "",
    val space: String = "",
    val name: String = "",
    val phone: String = "",
    val deleted: Boolean = false,
    val updated: String = "",
)

@Serializable
data class SettlementLineDto(
    val personId: String = "",
    val name: String = "",
    val net: Long = 0,
)

@Serializable
data class TransferDto(
    val fromId: String = "",
    val toId: String = "",
    val amount: Long = 0,
)

/** Corte/liquidación congelado de un espacio de Gastos. `date` en texto ISO (yyyy-MM-dd). */
@Serializable
data class SettlementDto(
    val id: String,
    val owner: String = "",
    val space: String = "",
    val title: String = "",
    val date: String = "",
    val total: Long = 0,
    val lines: List<SettlementLineDto> = emptyList(),
    val transfers: List<TransferDto> = emptyList(),
    val createdAt: Long = 0,
    val kind: String = "Corte",
    val settledBy: String = "",
    val deleted: Boolean = false,
    val updated: String = "",
)
