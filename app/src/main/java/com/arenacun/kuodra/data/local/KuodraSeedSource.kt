package com.arenacun.kuodra.data.local

import com.arenacun.kuodra.domain.model.AvatarTone
import com.arenacun.kuodra.domain.model.BudgetConfig
import com.arenacun.kuodra.domain.model.BudgetFrequency
import com.arenacun.kuodra.domain.model.FundConfig
import com.arenacun.kuodra.domain.model.Money
import com.arenacun.kuodra.domain.model.Movement
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.SettlementLine
import com.arenacun.kuodra.domain.model.SettlementRecord
import com.arenacun.kuodra.domain.model.SpaceSettings
import com.arenacun.kuodra.domain.model.UseCase
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Única fuente de datos en memoria del prototipo (reemplaza el seed que vivía en
 * `KuodraModel.kt` y el estado mutable global). Cuando lleguen Room/Ktor, esta clase queda detrás
 * de `data/local` y los repositorios combinan local + remoto sin tocar contratos ni UI.
 */
class KuodraSeedSource {

    private val deleted = MutableStateFlow<Set<String>>(emptySet())
    val deletedIds: StateFlow<Set<String>> = deleted.asStateFlow()

    private val added = MutableStateFlow<Map<UseCase, List<Movement>>>(emptyMap())
    val addedMovements: StateFlow<Map<UseCase, List<Movement>>> = added.asStateFlow()

    fun markDeleted(id: String) {
        deleted.value = deleted.value + id
    }

    fun addMovement(useCase: UseCase, movement: Movement) {
        val current = added.value[useCase].orEmpty()
        added.value = added.value + (useCase to (current + movement))
    }

    fun addedFor(useCase: UseCase): List<Movement> = added.value[useCase].orEmpty()

    /** Movimientos semilla por caso de uso (allMovements del prototipo). */
    fun baseMovements(useCase: UseCase): List<Movement> = when (useCase) {
        UseCase.Personal -> listOf(
            Movement("p1", money(340.0), "super", "Súper de la semana",
                note = "OXXO Roma Norte", date = LocalDate.of(2026, 6, 20)),
            Movement("p2", money(420.0), "restaurantes", "Comida con Sam",
                date = LocalDate.of(2026, 6, 19)),
            Movement("p3", money(118.0), "transporte", "Uber al centro",
                date = LocalDate.of(2026, 6, 2)),
        )
        UseCase.Gastos -> listOf(
            Movement("g1", money(8_000.0), "renta", "Renta de junio",
                date = LocalDate.of(2026, 6, 1), payer = "Andrea", splitNames = SPLIT5),
            Movement("g2", money(1_240.0), "super", "Súper de la semana",
                note = "Despensa quincenal", date = LocalDate.of(2026, 6, 3),
                payer = "Tú", splitNames = SPLIT5),
            Movement("g3", money(599.0), "servicios", "Internet",
                date = LocalDate.of(2026, 6, 5), payer = "Beto", splitNames = SPLIT5),
        )
        UseCase.Caja -> listOf(
            Movement("c1", money(340.0), "papeleria", "Papelería Office Depot",
                note = "Hojas y tóner", date = LocalDate.of(2026, 6, 20), payer = "Luis"),
            Movement("c2", money(500.0), "transporte", "Gasolina reparto",
                date = LocalDate.of(2026, 6, 19), payer = "Mar"),
            Movement("c3", money(180.0), "otro", "Café para junta",
                date = LocalDate.of(2026, 6, 19), payer = "Tú"),
        )
    }

    /** Personas del dashboard (people) — gastos y caja. */
    fun people(useCase: UseCase): List<Person> = when (useCase) {
        UseCase.Gastos -> listOf(
            Person("Andrea", "te debe", "+$450", true, "A", AvatarTone.Tint),
            Person("Caro", "te debe", "+$320", true, "C", AvatarTone.Pos),
            Person("Diego", "te debe", "+$320", true, "D", AvatarTone.Warn),
            Person("Beto", "le debes", "−$200", false, "B", AvatarTone.Neg),
        )
        UseCase.Caja -> listOf(
            Person("Luis", "6 movimientos", "$1,700", null, "L", AvatarTone.Tint),
            Person("Mar", "3 movimientos", "$1,300", null, "M", AvatarTone.Pos),
            Person("Tú", "4 movimientos · responsable", "$1,100", null, "T", AvatarTone.Warn),
        )
        UseCase.Personal -> emptyList()
    }

    /** Ajustes semilla por caso de uso (scrGroupSettings / scrPersonalSettings / scrCajaSettings). */
    fun settings(useCase: UseCase): SpaceSettings = when (useCase) {
        UseCase.Personal -> SpaceSettings(
            name = "Mis gastos",
            members = emptyList(),
            budget = BudgetConfig(
                enabled = false,
                frequency = BudgetFrequency.Biweekly,
                amount = "$6,000",
                firstDay = 1,
                secondDay = 16,
            ),
            fund = null,
            reminderEnabled = true,
        )
        UseCase.Gastos -> SpaceSettings(
            name = "Casa Roma",
            members = listOf(
                member("Tú", "Tú · admin", AvatarTone.Tint),
                member("Andrea", "Miembro", AvatarTone.Tint),
                member("Caro", "Miembro", AvatarTone.Pos),
                member("Beto", "Miembro", AvatarTone.Neg),
                member("Diego", "Miembro", AvatarTone.Warn),
            ),
            budget = null,
            fund = null,
            reminderEnabled = true,
        )
        UseCase.Caja -> SpaceSettings(
            name = "Caja Changarro",
            members = listOf(
                member("Tú", "Responsable", AvatarTone.Warn),
                member("Luis", "Autorizado", AvatarTone.Tint),
                member("Mar", "Autorizado", AvatarTone.Pos),
            ),
            budget = null,
            fund = FundConfig(initial = "$5,000"),
            reminderEnabled = true,
        )
    }

    private fun member(name: String, sub: String, tone: AvatarTone): Person =
        Person(name, sub, "", null, if (name == "Tú") "T" else name.take(1), tone)

    /** Historial de periodos cerrados (scrHistory). */
    fun history(useCase: UseCase): List<SettlementRecord> = when (useCase) {
        UseCase.Personal -> listOf(
            SettlementRecord("h-p1", "Quincena 1 · junio", "1–15 jun 2026", "$2,980", "16% bajo presupuesto",
                lines = listOf(
                    SettlementLine("Súper", "8 movimientos", "$1,420", AvatarTone.Tint, null),
                    SettlementLine("Restaurantes", "5 movimientos", "$980", AvatarTone.Pos, null),
                    SettlementLine("Transporte", "6 movimientos", "$580", AvatarTone.Warn, null),
                )),
            SettlementRecord("h-p2", "Quincena 2 · mayo", "16–31 may 2026", "$3,140", "4% sobre presupuesto",
                lines = listOf(
                    SettlementLine("Súper", "7 movimientos", "$1,610", AvatarTone.Tint, null),
                    SettlementLine("Ocio", "4 movimientos", "$890", AvatarTone.Neg, null),
                )),
        )
        UseCase.Gastos -> listOf(
            SettlementRecord("h-g1", "Liquidación de mayo", "1–31 may 2026", "$3,200", "Saldado · 4 miembros",
                lines = listOf(
                    SettlementLine("Andrea", "te pagó", "+$800", AvatarTone.Tint, true),
                    SettlementLine("Caro", "te pagó", "+$640", AvatarTone.Pos, true),
                    SettlementLine("Beto", "le pagaste", "−$420", AvatarTone.Neg, false),
                )),
            SettlementRecord("h-g2", "Liquidación de abril", "1–30 abr 2026", "$2,750", "Saldado · 4 miembros",
                lines = listOf(
                    SettlementLine("Diego", "te pagó", "+$610", AvatarTone.Warn, true),
                    SettlementLine("Beto", "te pagó", "+$320", AvatarTone.Neg, true),
                )),
        )
        UseCase.Caja -> listOf(
            SettlementRecord("h-c1", "Corte de mayo", "1–31 may 2026", "$4,820", "Conciliado · sin faltante",
                lines = listOf(
                    SettlementLine("Luis", "8 movimientos", "$2,100", AvatarTone.Tint, null),
                    SettlementLine("Mar", "5 movimientos", "$1,540", AvatarTone.Pos, null),
                    SettlementLine("Tú", "6 movimientos", "$1,180", AvatarTone.Warn, null),
                )),
            SettlementRecord("h-c2", "Corte de abril", "1–30 abr 2026", "$4,310", "Conciliado · faltante $40",
                lines = listOf(
                    SettlementLine("Luis", "7 movimientos", "$1,980", AvatarTone.Tint, null),
                    SettlementLine("Mar", "6 movimientos", "$1,620", AvatarTone.Pos, null),
                )),
        )
    }

    private companion object {
        val SPLIT5 = listOf("Tú", "Andrea", "Caro", "Beto", "Diego")
        fun money(value: Double): Money = Money.ofMajor(value)
    }
}
