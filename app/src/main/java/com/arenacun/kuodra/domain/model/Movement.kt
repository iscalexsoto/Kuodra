package com.arenacun.kuodra.domain.model

import com.arenacun.kuodra.domain.scan.ScanSource
import java.time.LocalDate

/**
 * Movimiento (gasto/ingreso). Modelo **data-shaped**, persistible y sincronizable: el monto es
 * numérico ([Money]) y la categoría es una **referencia** ([categoryId]). Los textos de
 * presentación (monto con formato, meta, fecha legible, perHead, verbo) NO se guardan: se derivan
 * en la capa de presentación (`MovementUi`).
 */
data class Movement(
    val id: String,
    val amount: Money,
    val categoryId: String,
    val title: String,
    val note: String = "",
    /** Fecha real del movimiento (para agrupar/filtrar y para los periodos de presupuesto). */
    val date: LocalDate = LocalDate.now(),
    /** Espacio de Gastos al que pertenece; `""` = Personal. */
    val spaceId: String = "",
    /** Quién puso el dinero (Gastos); puede haber varios pagadores. Vacío en Personal. */
    val payers: List<PayerShare> = emptyList(),
    /** Modo de división elegido en la captura (para re-poblar la UI al editar). */
    val splitMode: SplitMode = SplitMode.None,
    /** División ya resuelta a centavos exactos (Gastos); suma el total. Vacío en Personal. */
    val splits: List<SplitShare> = emptyList(),
    /** Id del corte que liquidó este gasto; `""` = vivo (cuenta en los balances). */
    val settlementId: String = "",
    /** Desglose interno opcional en partidas (concepto + cantidad). Vacío = sin detalle. */
    val items: List<MovementItem> = emptyList(),
    /** Raw OCR del ticket si el movimiento nació de un escaneo (material para templates futuros). */
    val scanRawText: String? = null,
    /** Origen del escaneo (cámara/galería); null = captura manual. */
    val scanSource: ScanSource? = null,
)

/**
 * Partida de un desglose: una línea (concepto + cantidad) dentro de un [Movement]. El remanente
 * no detallado (total − suma de partidas) se calcula con [adjustmentOf] y se muestra como
 * "Ajuste"; no se guarda como partida.
 */
data class MovementItem(
    val id: String,
    val concept: String,
    val amount: Money,
    /** Gastos (futuro): quién pagó esta partida; null = el pagador del movimiento. */
    val payer: String? = null,
)

/** Monto no detallado: total − suma de partidas (puede ser negativo si exceden el total). */
fun adjustmentOf(total: Money, items: List<MovementItem>): Money =
    total - items.map { it.amount }.total()

/** Inicial(es) a partir de un nombre. */
fun initialsOf(name: String): String = name.trim().take(1).uppercase()

/** Tono de avatar determinístico por nombre conocido. */
fun toneForName(name: String): AvatarTone = when (name) {
    "Tú" -> AvatarTone.Tint
    "Andrea" -> AvatarTone.Tint
    "Caro" -> AvatarTone.Pos
    "Diego" -> AvatarTone.Warn
    "Beto" -> AvatarTone.Neg
    else -> AvatarTone.Tint
}
