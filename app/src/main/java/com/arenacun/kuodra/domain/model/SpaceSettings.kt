package com.arenacun.kuodra.domain.model

/** Frecuencia del presupuesto personal (`scrPersonalSettings` del prototipo). */
enum class BudgetFrequency(val label: String) {
    Weekly("Semanal"), Biweekly("Quincenal"), Monthly("Mensual"), Custom("Personalizado")
}

/**
 * Configuración del presupuesto (solo Personal). Cada frecuencia usa su propio campo de día:
 * [weekday] (Semanal), [firstDay]+[secondDay] (Quincenal), [monthlyDay] (Mensual) e
 * [customInterval] (Personalizado). Réplica de `scrPersonalSettings` del prototipo.
 */
data class BudgetConfig(
    val enabled: Boolean,
    val frequency: BudgetFrequency,
    /** Monto límite formateado, p. ej. "$6,000". */
    val amount: String,
    /** Semanal: día de la semana en que empieza el periodo (1 = lunes … 7 = domingo). */
    val weekday: Int = 1,
    /** Quincenal: primer día de ingreso (1..28). */
    val firstDay: Int = 1,
    /** Quincenal: segundo día de ingreso (1..31). */
    val secondDay: Int = 16,
    /** Mensual: día del mes de ingreso (1..31). */
    val monthlyDay: Int = 1,
    /** Personalizado: cierra el periodo cada N días. */
    val customInterval: Int = 15,
) {
    companion object {
        /** Presupuesto por defecto (apagado) de un usuario nuevo. */
        val Default = BudgetConfig(
            enabled = false,
            frequency = BudgetFrequency.Biweekly,
            amount = "$6,000",
            firstDay = 1,
            secondDay = 16,
        )
    }
}

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
