package com.arenacun.kuodra.domain.model

/** País con su código de marcación para el teléfono de WhatsApp. Puro. */
data class Country(val name: String, val flag: String, val dialCode: String)

/**
 * Lista curada de países para el selector de código de teléfono (default México). El teléfono se
 * guarda como `dialCode + número local` (solo dígitos), formato que `wa.me` consume tras quitar el
 * `+`. [split] separa un teléfono guardado en (código, número local) para pre-poblar la edición.
 */
object Countries {

    val MEXICO = Country("México", "🇲🇽", "+52")

    val ALL: List<Country> = listOf(
        MEXICO,
        Country("EE.UU. / Canadá", "🇺🇸", "+1"),
        Country("Guatemala", "🇬🇹", "+502"),
        Country("El Salvador", "🇸🇻", "+503"),
        Country("Honduras", "🇭🇳", "+504"),
        Country("Colombia", "🇨🇴", "+57"),
        Country("Argentina", "🇦🇷", "+54"),
        Country("Chile", "🇨🇱", "+56"),
        Country("Perú", "🇵🇪", "+51"),
        Country("España", "🇪🇸", "+34"),
    )

    val DEFAULT: Country = MEXICO

    fun byDialCode(dialCode: String): Country = ALL.firstOrNull { it.dialCode == dialCode } ?: DEFAULT

    /**
     * Separa un teléfono guardado (`+52155…`) en (dialCode, número local). Usa el código más largo que
     * coincida como prefijo (para no confundir `+1` con `+52`). Vacío o sin coincidencia ⇒ default MX.
     */
    fun split(phone: String): Pair<String, String> {
        val digits = phone.filter { it.isDigit() }
        if (digits.isEmpty()) return DEFAULT.dialCode to ""
        val match = ALL
            .sortedByDescending { it.dialCode.length }
            .firstOrNull { digits.startsWith(it.dialCode.filter { ch -> ch.isDigit() }) }
        return if (match != null) {
            match.dialCode to digits.removePrefix(match.dialCode.filter { it.isDigit() })
        } else {
            DEFAULT.dialCode to digits
        }
    }
}
