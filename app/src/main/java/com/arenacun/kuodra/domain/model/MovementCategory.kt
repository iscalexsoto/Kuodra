package com.arenacun.kuodra.domain.model

/**
 * Categoría seleccionable al registrar un movimiento (sheet `sheetCategory` del prototipo).
 * [tag] es la etiqueta corta del [CategoryTag]; [tone] mapea a la paleta del tema.
 */
data class MovementCategory(
    val name: String,
    val tag: String,
    val tone: AvatarTone,
) {
    companion object {
        /** Catálogo del selector (colores del handoff: Comida morado, Servicios verde, etc.). */
        val defaults: List<MovementCategory> = listOf(
            MovementCategory("Comida", "Co", AvatarTone.Tint),
            MovementCategory("Transporte", "Tr", AvatarTone.Warn),
            MovementCategory("Servicios", "Se", AvatarTone.Pos),
            MovementCategory("Gasolina", "Ga", AvatarTone.Warn),
            MovementCategory("Ocio", "Oc", AvatarTone.Neg),
        )
    }
}
