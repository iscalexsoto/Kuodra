package com.arenacun.kuodra.domain.model

/**
 * Categoría de un movimiento. Entidad de catálogo **creada por el usuario** (referenciada por
 * [Movement.categoryId]); el desglose por categoría del dashboard se computa de los movimientos.
 *
 * El catálogo del usuario arranca vacío salvo la categoría estática [Uncategorized] ("Sin
 * categoría"), que es el default de todo movimiento y no puede editarse ni borrarse. Reportar
 * movimientos sin categoría = filtrar por su id.
 */
data class Category(
    val id: String,
    val name: String,
    val tag: String,
    val tone: AvatarTone,
    val archived: Boolean = false,
) {
    /** La categoría estática "Sin categoría" no se edita ni se borra. */
    val isStatic: Boolean get() = id == Uncategorized.id

    companion object {
        /** Categoría por defecto, siempre presente, no editable ni borrable. */
        val Uncategorized = Category("uncategorized", "Sin categoría", "Sc", AvatarTone.Tint)

        /**
         * Categorías "de fábrica" SOLO para resolver ids legacy/seed (p. ej. los movimientos
         * semilla de Gastos/Caja). No forman parte del catálogo del usuario ni del selector.
         */
        private val builtIns: List<Category> = listOf(
            Category("comida", "Comida", "Co", AvatarTone.Tint),
            Category("super", "Súper", "Sú", AvatarTone.Tint),
            Category("restaurantes", "Restaurantes", "Re", AvatarTone.Pos),
            Category("transporte", "Transporte", "Tr", AvatarTone.Warn),
            Category("gasolina", "Gasolina", "Ga", AvatarTone.Warn),
            Category("servicios", "Servicios", "Se", AvatarTone.Pos),
            Category("ocio", "Ocio", "Oc", AvatarTone.Neg),
            Category("renta", "Renta", "Rn", AvatarTone.Tint),
            Category("papeleria", "Papelería", "Pa", AvatarTone.Tint),
            Category("otro", "Otro", "Ot", AvatarTone.Warn),
        )

        /** Resuelve un id; cae a [Uncategorized] si no existe (datos viejos o aún sin cargar). */
        fun byId(id: String): Category =
            if (id == Uncategorized.id) Uncategorized
            else builtIns.find { it.id == id } ?: Uncategorized

        /** Etiqueta corta (2 letras) derivada del nombre para categorías nuevas. */
        fun deriveTag(name: String): String =
            name.trim().take(2).replaceFirstChar { it.uppercaseChar() }
    }
}
