package com.arenacun.kuodra.domain.model

/**
 * Espacio activo del usuario: el caso de uso seleccionado y el nombre elegido.
 * Sustituye al `useCase` + `spaceName` que vivían en el god-object del prototipo.
 */
data class Space(
    val useCase: UseCase,
    val name: String = "",
) {
    val terminology: Terminology get() = terminologyFor(useCase)
    val displayName: String get() = name.ifBlank { terminology.groupName }
}
