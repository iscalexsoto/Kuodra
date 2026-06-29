package com.arenacun.kuodra.presentation.feature.movement

import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.Calc
import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.DateLabels
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.UseCase
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
)

/** Reparto de un movimiento dividido entre varias personas. */
data class SplitShare(val name: String, val initials: String, val share: String, val tone: AvatarTone)

/** Grupo de día para "Ver todo", ya proyectado a UI. */
data class MovementGroupUi(val header: String, val movements: List<MovementUi>)

/** Deriva la proyección de UI a partir del catálogo de categorías y el caso de uso. */
fun Movement.toUi(
    categories: Map<String, Category>,
    useCase: UseCase,
    today: LocalDate,
): MovementUi {
    val cat = categories[categoryId] ?: Category.byId(categoryId)
    val byVerb = payer?.let {
        when (useCase) {
            UseCase.Gastos -> if (it == "Tú") "Pagaste" else "Pagó"
            UseCase.Caja -> if (it == "Tú") "Reportaste" else "Reportó"
            UseCase.Personal -> null
        }
    }
    val perHead = if (splitNames.isNotEmpty()) Calc.formatAmount(amount.major / splitNames.size) else null
    val meta = buildString {
        if (payer != null && byVerb != null) append("$byVerb $payer · ")
        append(DateLabels.dayMonth(date))
    }
    val shares = splitNames.map { name ->
        SplitShare(name, if (name == "Tú") "T" else name.take(1), perHead.orEmpty(), toneForName(name))
    }
    return MovementUi(
        id = id,
        title = title.ifBlank { cat.name },
        meta = meta,
        amount = Calc.formatAmount(amount.major),
        catTag = cat.tag,
        catName = cat.name,
        tone = cat.tone,
        dateStr = DateLabels.longLabel(date, today),
        by = payer,
        byVerb = byVerb,
        splitShares = shares,
        perHead = perHead,
        note = note,
    )
}

/** Proyecta un grupo de día completo a UI. */
fun MovementGroup.toUi(categories: Map<String, Category>, useCase: UseCase, today: LocalDate): MovementGroupUi =
    MovementGroupUi(header, movements.map { it.toUi(categories, useCase, today) })
