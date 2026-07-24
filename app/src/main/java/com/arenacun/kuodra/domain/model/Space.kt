package com.arenacun.kuodra.domain.model

/**
 * Espacio activo del usuario. Personal es único y sintético ([PERSONAL], sin fila); los de Gastos
 * son filas reales ([id] no vacío) y multi-instancia (varios grupos a la vez), archivables.
 */
data class Space(
    val id: String,
    val useCase: UseCase,
    val name: String = "",
    val archived: Boolean = false,
    /** Recordatorio de liquidación (solo Gastos). */
    val reminderEnabled: Boolean = true,
) {
    val terminology: Terminology get() = terminologyFor(useCase)
    val displayName: String get() = name.ifBlank { terminology.groupName }

    companion object {
        /** Espacio Personal (único, sin fila en Room/PocketBase). Su id es `""`. */
        val PERSONAL = Space(id = "", useCase = UseCase.Personal)
    }
}
