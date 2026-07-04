package com.arenacun.kuodra.data.scan

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrRowsTest {

    /**
     * Reproduce el caso real del emulador: MLKit devuelve las columnas como bloques separados
     * (conceptos primero, montos después, y el encabezado grande como bloque suelto). La
     * reconstrucción debe re-emparejar cada monto con su concepto por geometría.
     */
    @Test
    fun `re-pairs two-column ticket lines into reading order`() {
        val lines = listOf(
            // Bloque 1: columna de conceptos (izquierda)
            OcrLine("COCA COLA 600ML", top = 200, bottom = 230, left = 40),
            OcrLine("SABRITAS 52G", top = 260, bottom = 290, left = 40),
            OcrLine("TOTAL", top = 340, bottom = 370, left = 40),
            // Bloque 2: encabezado (fuente grande, bloque aparte)
            OcrLine("OXXO", top = 20, bottom = 60, left = 240),
            // Bloque 3: columna de montos (derecha)
            OcrLine("19.00", top = 202, bottom = 228, left = 400),
            OcrLine("18.50", top = 262, bottom = 288, left = 400),
            OcrLine("59.50", top = 342, bottom = 368, left = 400),
        )

        assertEquals(
            """
            OXXO
            COCA COLA 600ML  19.00
            SABRITAS 52G  18.50
            TOTAL  59.50
            """.trimIndent(),
            reconstructRows(lines),
        )
    }

    @Test
    fun `empty input yields empty text`() {
        assertEquals("", reconstructRows(emptyList()))
    }

    @Test
    fun `single column stays in vertical order`() {
        val lines = listOf(
            OcrLine("B", top = 100, bottom = 120, left = 10),
            OcrLine("A", top = 10, bottom = 30, left = 10),
            OcrLine("C", top = 200, bottom = 220, left = 10),
        )
        assertEquals("A\nB\nC", reconstructRows(lines))
    }
}
