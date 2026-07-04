package com.arenacun.kuodra.domain.scan

/**
 * PUNTO ÚNICO de normalización del raw OCR antes de entrar a la cadena de [TicketParser]. Aquí se
 * acumulan las correcciones de reconocimiento (unificar separadores, compactar montos partidos por
 * espacios; en el futuro: confusiones O/0, S/5…). Función pura: mismo raw ⇒ mismo resultado, así
 * el parseo es reproducible a partir del `rawText` persistido.
 */
object OcrNormalizer {

    /** Espacios alrededor de un separador entre dígitos: "1 , 234 . 56" → "1,234.56". */
    private val spacedSeparator = Regex("""(?<=\d)\s*([.,])\s*(?=\d)""")

    /** Espacios alrededor de separadores de fecha: "30/06/ 2026" → "30/06/2026". */
    private val spacedDateSeparator = Regex("""(?<=\d)\s*([/-])\s*(?=\d)""")

    /** Espacio entre el símbolo de moneda y el número: "$ 120.00" → "$120.00". */
    private val spacedCurrency = Regex("""\$\s+(?=\d)""")

    /** Confusión OCR 0↔O dentro/al final de una palabra: "CAMBI0" → "CAMBIO", "0XXO" → "OXXO". */
    private val zeroInWord = Regex("""(?<=[A-Za-zÁÉÍÓÚÑáéíóúñ]{2})0(?=[A-Za-zÁÉÍÓÚÑáéíóúñ]|\b)""")
    private val zeroBeforeWord = Regex("""\b0(?=[A-Za-zÁÉÍÓÚÑáéíóúñ]{2})""")

    private val multiSpace = Regex("""\s+""")

    fun normalize(raw: String): String {
        val lines = raw.lines().map { line ->
            line
                .replace('–', '-').replace('—', '-')   // guiones unicode → '-'
                .replace('‘', '\'').replace('’', '\'') // comillas unicode → ASCII
                .replace('“', '"').replace('”', '"')
                .replace(multiSpace, " ")
                .replace(spacedSeparator, "$1")
                .replace(spacedDateSeparator, "$1")
                .replace(spacedCurrency, Regex.escapeReplacement("$"))
                .replace(zeroInWord, "O")
                .replace(zeroBeforeWord, "O")
                .trim()
        }
        // Colapsa rachas de líneas vacías en una sola y recorta extremos.
        val out = mutableListOf<String>()
        for (line in lines) {
            if (line.isEmpty() && out.lastOrNull()?.isEmpty() != false) continue
            out += line
        }
        while (out.lastOrNull()?.isEmpty() == true) out.removeAt(out.lastIndex)
        return out.joinToString("\n")
    }
}
