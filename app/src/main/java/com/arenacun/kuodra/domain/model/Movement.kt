package com.arenacun.kuodra.domain.model

import java.time.LocalDate

data class Movement(
    val id: String,
    val title: String,
    val meta: String,
    val amount: String,
    val catTag: String,
    val catName: String,
    val tone: AvatarTone,
    val dateStr: String,
    val by: String? = null,        // quién pagó/reportó
    val byVerb: String? = null,    // "Pagó" / "Reportó" / "Pagaste"
    val splitNames: List<String> = emptyList(),
    val perHead: String? = null,
    val note: String = "",
    /** Fecha real del movimiento (para agrupar/filtrar en "Ver todo"). */
    val date: LocalDate = LocalDate.now(),
)

/** Reparto de un movimiento dividido entre varias personas. */
data class SplitShare(val name: String, val initials: String, val share: String, val tone: AvatarTone)

/** Inicial(es) a partir de un nombre. */
fun initialsOf(name: String): String = name.trim().take(1).uppercase()

/** Tono de avatar determinístico por nombre conocido. */
fun toneForName(name: String): AvatarTone = when (name) {
    "Tú" -> AvatarTone.Tint
    "Andrea", "Luis" -> AvatarTone.Tint
    "Caro", "Mar" -> AvatarTone.Pos
    "Diego", "Sol" -> AvatarTone.Warn
    "Beto" -> AvatarTone.Neg
    else -> AvatarTone.Tint
}

/** Derivación pura: repartos de un movimiento dividido. */
fun Movement.splitShares(): List<SplitShare> =
    splitNames.map { name ->
        SplitShare(name, if (name == "Tú") "T" else name.take(1), perHead ?: "", toneForName(name))
    }
