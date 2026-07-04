package com.arenacun.kuodra.data.scan

/**
 * Línea de OCR con su caja (coordenadas de píxel de la imagen). Neutral para poder testear la
 * reconstrucción de filas en host sin tipos de Android.
 */
internal data class OcrLine(
    val text: String,
    val top: Int,
    val bottom: Int,
    val left: Int,
) {
    val centerY: Int get() = (top + bottom) / 2
}

/**
 * MLKit devuelve los **bloques** en orden arbitrario (en tickets de dos columnas suele soltar
 * primero todos los conceptos y luego todos los montos, rompiendo la asociación por renglón).
 * Reconstruye el orden de lectura por geometría: agrupa las líneas cuyo centro vertical cae
 * dentro de otra ya colocada (misma fila) y ordena cada fila por X, uniendo con dos espacios.
 */
internal fun reconstructRows(lines: List<OcrLine>): String {
    if (lines.isEmpty()) return ""
    val rows = mutableListOf<MutableList<OcrLine>>()
    for (line in lines.sortedBy { it.centerY }) {
        val row = rows.lastOrNull()
        if (row != null && row.any { line.centerY in it.top..it.bottom || it.centerY in line.top..line.bottom }) {
            row.add(line)
        } else {
            rows.add(mutableListOf(line))
        }
    }
    return rows.joinToString("\n") { row ->
        row.sortedBy { it.left }.joinToString("  ") { it.text }
    }
}
