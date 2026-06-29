package com.arenacun.kuodra.domain.model

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Motor puro de la calculadora del numpad (`openCalc` del prototipo). Sin Android ni
 * Compose: vive en `domain` para poder testearse en host. La UI (`KuodraCalculator`)
 * solo dibuja el estado y reenvía las pulsaciones como [CalcKey].
 */
enum class CalcKey { N0, N1, N2, N3, N4, N5, N6, N7, N8, N9, Dot, Plus, Minus, Times, Div, Clear, Back, Equals }

/**
 * Estado inmutable de la calculadora. [expression] guarda la expresión en ASCII
 * (`+ - * /`); [display] la presenta con los signos del prototipo (× ÷ −).
 */
data class CalcState(val expression: String = "") {

    /** Texto grande del display: el resultado si la expresión está resuelta, o la expresión. */
    val display: String
        get() = if (expression.isEmpty()) "0" else expression
            .replace('*', '×')
            .replace('/', '÷')

    /** Resultado evaluado, o `null` si la expresión está incompleta/inválida. */
    val result: Double? get() = Calc.evaluate(expression)

    /** Monto formateado listo para el formulario (p. ej. "$1,240"), o null si no hay resultado. */
    val formattedAmount: String? get() = result?.let { Calc.formatAmount(it) }
}

object Calc {

    private val operators = setOf('+', '-', '*', '/')

    fun press(state: CalcState, key: CalcKey): CalcState = when (key) {
        CalcKey.Clear -> CalcState()
        CalcKey.Back -> CalcState(state.expression.dropLast(1))
        CalcKey.Equals -> state.result?.let { CalcState(trimNumber(it)) } ?: state
        CalcKey.Dot -> appendDot(state)
        CalcKey.Plus -> appendOperator(state, '+')
        CalcKey.Minus -> appendOperator(state, '-')
        CalcKey.Times -> appendOperator(state, '*')
        CalcKey.Div -> appendOperator(state, '/')
        else -> appendDigit(state, digitOf(key))
    }

    private fun digitOf(key: CalcKey): Char = when (key) {
        CalcKey.N0 -> '0'; CalcKey.N1 -> '1'; CalcKey.N2 -> '2'; CalcKey.N3 -> '3'
        CalcKey.N4 -> '4'; CalcKey.N5 -> '5'; CalcKey.N6 -> '6'; CalcKey.N7 -> '7'
        CalcKey.N8 -> '8'; CalcKey.N9 -> '9'
        else -> error("not a digit: $key")
    }

    private fun appendDigit(state: CalcState, d: Char): CalcState =
        CalcState(state.expression + d)

    private fun appendDot(state: CalcState): CalcState {
        val token = currentToken(state.expression)
        if (token.contains('.')) return state               // ya hay punto en este número
        val base = if (token.isEmpty()) state.expression + "0" else state.expression
        return CalcState("$base.")
    }

    private fun appendOperator(state: CalcState, op: Char): CalcState {
        val expr = state.expression
        if (expr.isEmpty()) return state                     // no se empieza con operador
        val last = expr.last()
        if (last in operators) return CalcState(expr.dropLast(1) + op) // reemplaza operador
        return CalcState(expr + op)
    }

    /** Dígitos del número que se está escribiendo (tras el último operador). */
    private fun currentToken(expr: String): String =
        expr.takeLastWhile { it !in operators }

    /** Evalúa una expresión aritmética simple con precedencia estándar (× ÷ sobre + −). */
    fun evaluate(expression: String): Double? {
        val s = expression.trim()
        if (s.isEmpty() || s.last() in operators) return null

        val nums = mutableListOf<Double>()
        val ops = mutableListOf<Char>()
        val sb = StringBuilder()

        fun flush(): Boolean {
            if (sb.isEmpty()) return false
            val d = sb.toString().toDoubleOrNull() ?: return false
            nums.add(d); sb.clear(); return true
        }

        for (ch in s) when (ch) {
            in '0'..'9', '.' -> sb.append(ch)
            '+', '-', '*', '/' -> {
                if (sb.isEmpty()) {
                    if (ch == '-' && nums.isEmpty()) { sb.append(ch); continue } // negativo inicial
                    return null
                }
                if (!flush()) return null
                ops.add(ch)
            }
            else -> return null
        }
        if (!flush()) return null
        if (nums.size != ops.size + 1) return null

        // Primer paso: × y ÷
        val acc = mutableListOf(nums[0])
        val addSub = mutableListOf<Char>()
        for (k in ops.indices) {
            when (val op = ops[k]) {
                '*' -> acc[acc.lastIndex] = acc.last() * nums[k + 1]
                '/' -> {
                    if (nums[k + 1] == 0.0) return null
                    acc[acc.lastIndex] = acc.last() / nums[k + 1]
                }
                else -> { addSub.add(op); acc.add(nums[k + 1]) }
            }
        }
        // Segundo paso: + y −
        var total = acc[0]
        for (k in addSub.indices) {
            total = if (addSub[k] == '+') total + acc[k + 1] else total - acc[k + 1]
        }
        return total
    }

    /** Representa un número como token de expresión limpio (sin ".0" sobrante). */
    private fun trimNumber(value: Double): String {
        val rounded = roundTo2(value)
        return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString()
        else rounded.toString()
    }

    /** Formatea un monto como en el prototipo: "$1,240" o "$119.80" (2 decimales si hay). */
    fun formatAmount(value: Double): String {
        val rounded = roundTo2(value)
        val negative = rounded < 0
        val abs = abs(rounded)
        val whole = abs.toLong()
        val cents = ((abs - whole) * 100).roundToLong()
        val grouped = groupThousands(whole)
        val body = if (cents == 0L) grouped else "$grouped.${cents.toString().padStart(2, '0')}"
        return (if (negative) "-$" else "$") + body
    }

    /** Inverso de [formatAmount]: lee un monto formateado ("$6,000", "−$119.80") a número. */
    fun parseAmount(text: String): Double? =
        text.replace("$", "").replace(",", "").replace("−", "-").trim().toDoubleOrNull()

    private fun roundTo2(value: Double): Double = (value * 100).roundToLong() / 100.0

    private fun groupThousands(n: Long): String {
        val digits = n.toString()
        val sb = StringBuilder()
        for ((i, ch) in digits.withIndex()) {
            if (i > 0 && (digits.length - i) % 3 == 0) sb.append(',')
            sb.append(ch)
        }
        return sb.toString()
    }
}
