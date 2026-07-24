package com.arenacun.kuodra.domain.model

import com.arenacun.kuodra.domain.usecase.ReturnCalc

/** Frecuencia del presupuesto personal (`scrPersonalSettings` del prototipo). */
enum class BudgetFrequency(val label: String) {
    Weekly("Semanal"), Biweekly("Quincenal"), Monthly("Mensual"), Custom("Personalizado")
}

/**
 * Configuración del presupuesto y las devoluciones (solo Personal). Cada frecuencia usa su propio
 * campo de día: [weekday] (Semanal), [firstDay]+[secondDay] (Quincenal), [monthlyDay] (Mensual) e
 * [customInterval] (Personalizado). Réplica de `scrPersonalSettings` del prototipo, extendida con
 * [returnPercent] (el % global vivo de los movimientos "Por devolver").
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
    /** % global a devolver de los movimientos "Por devolver" (5..100). Independiente de [enabled]. */
    val returnPercent: Int = ReturnCalc.DEFAULT_RETURN_PERCENT,
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

/**
 * Ajustes del espacio. Un único modelo que cubre los `scr*Settings` del prototipo:
 * el contenido relevante cambia por caso de uso (presupuesto en Personal, miembros y
 * recordatorio en Gastos).
 */
data class SpaceSettings(
    val name: String,
    /** Miembros (vacío en Personal). */
    val members: List<Person>,
    /** Presupuesto (solo Personal; null en el resto). */
    val budget: BudgetConfig?,
    val reminderEnabled: Boolean,
)
