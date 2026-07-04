package com.arenacun.kuodra.domain.scan

import com.arenacun.kuodra.domain.model.Money
import java.time.LocalDate

/**
 * Fallback local de parseo de tickets: heurísticas regex puras sobre el texto OCR normalizado.
 * Es el eslabón **terminal** de la cadena: nunca devuelve `null` — entrega un [ParsedTicket]
 * parcial (o vacío) y el usuario completa en el formulario. Kotlin puro, testeable en host.
 *
 * `today` se inyecta para poder fijarlo en tests; las fechas futuras se descartan (un ticket no
 * puede ser de mañana; suelen ser errores de OCR).
 */
class RegexTicketParser(
    private val today: () -> LocalDate = LocalDate::now,
) : TicketParser {

    override val source: TicketParseSource = TicketParseSource.Regex

    override suspend fun parse(normalizedText: String): ParsedTicket {
        val lines = normalizedText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return ParsedTicket(
            merchant = findMerchant(lines),
            total = findTotal(lines),
            date = findDate(lines, today()),
            items = findItems(lines),
            source = source,
        )
    }

    companion object {
        /**
         * Monto con dos decimales: "187.50", "1,234.56", "1.234,56". El lookahead evita leer
         * "12,34" dentro de "12,345" (miles sin decimales no son monto).
         */
        private val amount = Regex("""\d+(?:[.,]\d{3})*[.,]\d{2}(?!\d)""")

        private val totalLabel = Regex("""(?i)\b(GRAN\s*TOTAL|TOTAL(\s*A\s*PAGAR)?|IMPORTE)\b""")
        private val subtotalLabel = Regex("""(?i)SUB\s*-?\s*TOTAL""")

        /** Líneas que no son partidas aunque terminen en monto (totales, impuestos, pagos). */
        private val itemExclusions = Regex(
            """(?i)\b(TOTAL|SUBTOTAL|IVA|CAMBIO|EFECTIVO|TARJETA|PROPINA|DESCUENTO|PUNTOS|SALDO|IMPORTE|PAGO)\b""",
        )

        /** Partida: texto + monto al final de la línea. */
        private val itemLine = Regex("""^(.{3,40}?)\s+\$?(\d+(?:[.,]\d{3})*[.,]\d{2}(?!\d))\s*$""")

        /** Palabras de las primeras líneas que delatan datos fiscales/contacto, no el comercio. */
        private val merchantExclusions = Regex("""(?i)\b(RFC|SUC|SUCURSAL|TEL|TELEFONO|FACTURA|TICKET|FOLIO|CAJA)\b""")

        private val numericDate = Regex("""\b(\d{1,2})[/-](\d{1,2})[/-](\d{2}(?:\d{2})?)\b""")
        private val isoDate = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""")
        private val spanishDate = Regex(
            """(?i)\b(\d{1,2})\s+de\s+(enero|febrero|marzo|abril|mayo|junio|julio|agosto|septiembre|octubre|noviembre|diciembre)\s+(?:de\s+)?(\d{4})\b""",
        )
        private val monthNames = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        )

        /** Convierte un match de [amount] a centavos: el último `[.,]` es el separador decimal. */
        internal fun toCents(text: String): Long {
            val decimalAt = text.indexOfLast { it == '.' || it == ',' }
            val major = text.take(decimalAt).filter { it.isDigit() }
            val minor = text.substring(decimalAt + 1)
            return (major.ifEmpty { "0" }).toLong() * 100 + minor.toLong()
        }

        private fun amountsIn(line: String): List<Long> = amount.findAll(line).map { toCents(it.value) }.toList()

        /**
         * Total: la última línea etiquetada (TOTAL/IMPORTE, no SUBTOTAL) con monto en ella o en la
         * inmediata siguiente (el OCR suele partir etiqueta/valor); si no hay etiqueta, el monto
         * máximo del documento (el total ≥ cualquier partida).
         */
        internal fun findTotal(lines: List<String>): Money? {
            var labeled: Long? = null
            lines.forEachIndexed { i, line ->
                if (!totalLabel.containsMatchIn(line) || subtotalLabel.containsMatchIn(line)) return@forEachIndexed
                val inLine = amountsIn(line).lastOrNull()
                    ?: lines.getOrNull(i + 1)?.let { next ->
                        amountsIn(next).firstOrNull().takeIf { !subtotalLabel.containsMatchIn(next) }
                    }
                if (inLine != null) labeled = inLine
            }
            labeled?.let { return Money(it) }
            return lines.flatMap { amountsIn(it) }.maxOrNull()?.let { Money(it) }
        }

        /** Primera fecha válida y no futura del documento. */
        internal fun findDate(lines: List<String>, today: LocalDate): LocalDate? {
            for (line in lines) {
                val candidate = parseDate(line)
                if (candidate != null && !candidate.isAfter(today)) return candidate
            }
            return null
        }

        private fun parseDate(line: String): LocalDate? {
            isoDate.find(line)?.let { m ->
                val (y, mo, d) = m.destructured
                dateOf(y.toInt(), mo.toInt(), d.toInt())?.let { return it }
            }
            numericDate.find(line)?.let { m ->
                val (d, mo, y) = m.destructured
                val year = y.toInt().let { if (it < 100) 2000 + it else it }
                dateOf(year, mo.toInt(), d.toInt())?.let { return it }
            }
            spanishDate.find(line)?.let { m ->
                val (d, month, y) = m.destructured
                val mo = monthNames.indexOf(month.lowercase()) + 1
                dateOf(y.toInt(), mo, d.toInt())?.let { return it }
            }
            return null
        }

        private fun dateOf(year: Int, month: Int, day: Int): LocalDate? =
            runCatching { LocalDate.of(year, month, day) }.getOrNull()

        /**
         * Comercio: primera de las 4 primeras líneas con pinta de nombre (3–40 chars, mayoría
         * letras, sin keywords fiscales ni montos/fechas).
         */
        internal fun findMerchant(lines: List<String>): String? =
            lines.take(4).firstOrNull { line ->
                line.length in 3..40 &&
                    line.count { it.isLetter() } > line.length / 2 &&
                    !merchantExclusions.containsMatchIn(line) &&
                    !amount.containsMatchIn(line) &&
                    parseDate(line) == null
            }

        /** Partidas: líneas `concepto … monto` que no sean totales/impuestos/pagos. */
        internal fun findItems(lines: List<String>): List<ParsedTicketItem> =
            lines.mapNotNull { line ->
                if (itemExclusions.containsMatchIn(line)) return@mapNotNull null
                val match = itemLine.find(line) ?: return@mapNotNull null
                val concept = match.groupValues[1].trim { it == '.' || it == '*' || it == '-' || it == ':' || it.isWhitespace() }
                val cents = toCents(match.groupValues[2])
                if (concept.none { it.isLetter() } || cents <= 0L) return@mapNotNull null
                ParsedTicketItem(concept, Money(cents))
            }
    }
}
