package com.arenacun.kuodra.domain.model

/**
 * Registro de contacto dentro de un espacio de Gastos compartidos. La app es **single-admin**: estas
 * personas no tienen cuenta ni conexión, solo son a quién se le reparte el gasto y a quién se le
 * cobra ([phone] para el enlace de WhatsApp). Kotlin puro.
 */
data class SpacePerson(
    val id: String,
    val name: String,
    /** Teléfono en formato internacional para `wa.me` (opcional; vacío = sin WhatsApp). */
    val phone: String = "",
)

/** Referencias de persona reservadas usadas en pagadores/divisiones/cortes. */
object PersonRef {
    /** El dueño de la app ("Tú"): participa en pagos y divisiones pero **no** es una fila de `persons`. */
    const val ME = "me"
}
