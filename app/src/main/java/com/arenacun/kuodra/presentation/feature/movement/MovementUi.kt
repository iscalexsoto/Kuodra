package com.arenacun.kuodra.presentation.feature.movement

import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.PersonRef
import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.model.adjustmentOf
import com.arenacun.kuodra.domain.model.initialsOf
import com.arenacun.kuodra.domain.model.toneForName
import com.arenacun.kuodra.domain.usecase.MovementGroup
import java.time.LocalDate

/**
 * Proyección de presentación de un [Movement]: aquí se **derivan** todos los textos de display
 * (monto con formato, meta, fecha legible, verbo, reparto) que el dominio ya no guarda. Las
 * pantallas consumen `MovementUi`, nunca el `Movement` crudo.
 */
data class MovementUi(
    val id: String,
    val title: String,
    val meta: String,
    val amount: String,
    val catTag: String,
    val catName: String,
    val tone: AvatarTone,
    val dateStr: String,
    val by: String?,
    val byVerb: String?,
    val splitShares: List<SplitShare>,
    val perHead: String?,
    val note: String,
    val items: List<MovementItemUi>,
    /** "Ajuste" (total no detallado) formateado, o null si el movimiento no tiene desglose. */
    val adjustment: String?,
)

/** Reparto de un movimiento dividido entre varias personas. */
data class SplitShare(val name: String, val initials: String, val share: String, val tone: AvatarTone)

/** Partida del desglose, ya proyectada a UI. */
data class MovementItemUi(val concept: String, val amount: String)

/** Grupo de día para "Ver todo", ya proyectado a UI. */
data class MovementGroupUi(val header: String, val movements: List<MovementUi>)

/**
 * Deriva la proyección de UI. [persons] resuelve id → nombre para los pagadores/división de Gastos
 * ([PersonRef.ME] siempre es "Tú"; un id ausente se pinta "(eliminado)").
 */
fun Movement.toUi(
    categories: Map<String, Category>,
    useCase: UseCase,
    today: LocalDate,
    persons: Map<String, String> = emptyMap(),
): MovementUi {
    val cat = categories[categoryId] ?: Category.byId(categoryId)
    fun nameOf(id: String): String = if (id == PersonRef.ME) "Tú" else persons[id] ?: "(eliminado)"

    val by: String?
    val byVerb: String?
    when {
        useCase != UseCase.Gastos || payers.isEmpty() -> { by = null; byVerb = null }
        payers.size == 1 && payers.first().personId == PersonRef.ME -> { by = "Tú"; byVerb = "Pagaste" }
        payers.size == 1 -> { by = nameOf(payers.first().personId); byVerb = "Pagó" }
        else -> { by = payers.joinToString(", ") { nameOf(it.personId) }; byVerb = "Pagaron" }
    }
    val payerMeta = when {
        byVerb == null -> null
        payers.size == 1 -> "$byVerb $by"
        else -> "Pagaron ${payers.size}"
    }
    val meta = listOfNotNull(payerMeta, DateLabels.dayMonth(date)).joinToString(" · ")

    val shares = splits.map { s ->
        val name = nameOf(s.personId)
        SplitShare(name, initialsOf(name), Calc.formatAmount(s.share.major), toneForName(name))
    }
    // "c/u" solo tiene sentido si todas las partes son iguales (reparto equitativo).
    val perHead = splits.map { it.share.cents }.distinct().singleOrNull()
        ?.let { Calc.formatAmount(it / 100.0) }
    val itemsUi = items.map { MovementItemUi(it.concept, Calc.formatAmount(it.amount.major)) }
    val adjustmentStr = if (items.isNotEmpty())
        Calc.formatAmount(adjustmentOf(amount, items).major) else null
    return MovementUi(
        id = id,
        title = title.ifBlank { cat.name },
        meta = meta,
        amount = Calc.formatAmount(amount.major),
        catTag = cat.tag,
        catName = cat.name,
        tone = cat.tone,
        dateStr = DateLabels.longLabel(date, today),
        by = by,
        byVerb = byVerb,
        splitShares = shares,
        perHead = perHead,
        note = note,
        items = itemsUi,
        adjustment = adjustmentStr,
    )
}

/** Proyecta un grupo de día completo a UI. */
fun MovementGroup.toUi(
    categories: Map<String, Category>,
    useCase: UseCase,
    today: LocalDate,
    persons: Map<String, String> = emptyMap(),
): MovementGroupUi =
    MovementGroupUi(header, movements.map { it.toUi(categories, useCase, today, persons) })
