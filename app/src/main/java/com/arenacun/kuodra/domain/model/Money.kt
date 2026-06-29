package com.arenacun.kuodra.domain.model

/**
 * Dinero en **unidades mínimas** (centavos). A diferencia del `String` "$340" del prototipo,
 * es numérico ⇒ se puede sumar, comparar y agregar (presupuestos, cortes). El **formateo** a
 * texto vive en [Calc.formatAmount]; aquí solo se guarda el valor. Kotlin puro: testeable en host.
 */
data class Money(val cents: Long) : Comparable<Money> {

    operator fun plus(other: Money): Money = Money(cents + other.cents)
    operator fun minus(other: Money): Money = Money(cents - other.cents)
    override fun compareTo(other: Money): Int = cents.compareTo(other.cents)

    /** Valor en unidades mayores (pesos) para formatear con [Calc.formatAmount]. */
    val major: Double get() = cents / 100.0

    companion object {
        val Zero = Money(0)

        /** Construye desde un valor en unidades mayores (p. ej. 119.80 → 11980 centavos). */
        fun ofMajor(value: Double): Money = Money(Math.round(value * 100))
    }
}

/** Suma una colección de montos preservando la precisión entera. */
fun Iterable<Money>.total(): Money = Money(sumOf { it.cents })
