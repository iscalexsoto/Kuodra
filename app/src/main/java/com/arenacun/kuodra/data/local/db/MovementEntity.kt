package com.arenacun.kuodra.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Fila persistida de un movimiento. Modelo de almacenamiento: monto en centavos y categoría por
 * referencia. Incluye las **columnas de sincronización** desde el día 1 (aunque el sync llega en
 * Fase 2) para no migrar el esquema después:
 * - [owner] id del usuario dueño (aislamiento multi-cuenta en el mismo dispositivo).
 * - [updatedAt] epoch millis del último cambio conocido (cursor de deltas + last-write-wins).
 * - [deleted] tombstone: borrado lógico para propagar la baja al sincronizar.
 * - [dirty] hay cambios locales sin subir.
 */
@Entity(tableName = "movements")
data class MovementEntity(
    @PrimaryKey val id: String,
    val owner: String,
    val amountCents: Long,
    val categoryId: String,
    val title: String,
    val note: String,
    val date: LocalDate,
    /** Espacio de Gastos al que pertenece; `""` = Personal. */
    val space: String = "",
    /** Pagadores del gasto serializados como JSON (`[]` = sin pagadores; Gastos). */
    val payersJson: String = "[]",
    /** Modo de división elegido en la captura (`None`/`Equal`/`Amount`/`Percent`). */
    val splitMode: String = "None",
    /** División resuelta a centavos, serializada como JSON (`[]` = sin división; Gastos). */
    val splitsJson: String = "[]",
    /** Id del corte que liquidó este gasto; `""` = vivo. */
    val settlementId: String = "",
    /** Desglose en partidas serializado como JSON (`[]` = sin detalle). */
    val itemsJson: String = "[]",
    /** Raw OCR del ticket si nació de un escaneo (material para templates futuros). */
    val scanRawText: String? = null,
    /** Nombre del enum `ScanSource` (`Camera`/`Gallery`); null = captura manual. */
    val scanSource: String? = null,
    /** Nombre del enum `ReturnStatus` (`None`/`Pending`/`Returned`). */
    val returnStatus: String = "None",
    /** % de devolución congelado al marcar `Returned`; null mientras None/Pending. */
    val returnPercent: Int? = null,
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
    /** `updated` del servidor de la última sincronización (vacío = nunca subido). */
    val remoteUpdated: String = "",
)
