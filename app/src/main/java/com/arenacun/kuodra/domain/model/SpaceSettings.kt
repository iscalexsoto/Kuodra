package com.arenacun.kuodra.domain.model

/** Frecuencia del presupuesto personal (`scrPersonalSettings` del prototipo). */
enum class BudgetFrequency(val label: String) {
    Weekly("Semanal"), Biweekly("Quincenal"), Monthly("Mensual"), Custom("Personalizado")
}

/** Configuración del presupuesto (solo Personal). */
data class BudgetConfig(
    val enabled: Boolean,
    val frequency: BudgetFrequency,
    /** Día de cierre del periodo (1..28). */
    val closingDay: Int,
    /** Monto límite formateado, p. ej. "$6,000". */
    val amount: String,
)

/** Configuración del fondo de caja chica (solo Caja). */
data class FundConfig(
    /** Monto inicial formateado, p. ej. "$5,000". */
    val initial: String,
)

/**
 * Ajustes del espacio. Un único modelo que cubre los tres `scr*Settings` del prototipo:
 * el contenido relevante cambia por caso de uso (presupuesto en Personal, miembros y
 * recordatorio en Gastos, fondo y autorizados en Caja).
 */
data class SpaceSettings(
    val name: String,
    /** Miembros / autorizados (vacío en Personal). */
    val members: List<Person>,
    /** Presupuesto (solo Personal; null en el resto). */
    val budget: BudgetConfig?,
    /** Fondo (solo Caja; null en el resto). */
    val fund: FundConfig?,
    val reminderEnabled: Boolean,
)
