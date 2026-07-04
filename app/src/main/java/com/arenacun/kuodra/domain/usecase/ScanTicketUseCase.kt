package com.arenacun.kuodra.domain.usecase

import com.arenacun.kuodra.domain.scan.OcrEngine
import com.arenacun.kuodra.domain.scan.OcrNormalizer
import com.arenacun.kuodra.domain.scan.ParsedTicket
import com.arenacun.kuodra.domain.scan.ScanSource
import com.arenacun.kuodra.domain.scan.TicketParseSource
import com.arenacun.kuodra.domain.scan.TicketParser
import com.arenacun.kuodra.domain.scan.TicketScan
import com.arenacun.kuodra.domain.telemetry.Telemetry

/**
 * Orquestador del escaneo de tickets: OCR → [OcrNormalizer] → cadena de [TicketParser] en orden
 * de prioridad (hoy `[Mistral, Regex]`; el futuro `Template` se insertará al frente vía DI, sin
 * tocar este código). Solo falla si el OCR falla; el parseo siempre produce algo porque el último
 * eslabón (regex) devuelve un resultado parcial o vacío.
 */
class ScanTicketUseCase(
    private val ocrEngine: OcrEngine,
    private val parsers: List<TicketParser>,
    private val telemetry: Telemetry,
) {

    suspend operator fun invoke(imageUri: String, scanSource: ScanSource): Result<TicketScan> {
        val raw = ocrEngine.recognize(imageUri).getOrElse {
            telemetry.capture(it, context = mapOf("scan.step" to "ocr", "scan.source" to scanSource.name))
            return Result.failure(it)
        }
        val normalized = OcrNormalizer.normalize(raw)
        val parsed = parsers.firstNotNullOfOrNull { parser ->
            runCatching { parser.parse(normalized) }.getOrNull()
        } ?: ParsedTicket(source = TicketParseSource.Regex)
        telemetry.breadcrumb(
            "scan",
            "ticket parsed via ${parsed.source}",
            mapOf(
                "source" to scanSource.name,
                "chars" to normalized.length.toString(),
                "empty" to parsed.isEmpty.toString(),
            ),
        )
        // El raw se conserva SIN normalizar: es el material fiel para los templates futuros.
        return Result.success(TicketScan(rawText = raw, parsed = parsed, scanSource = scanSource))
    }
}
