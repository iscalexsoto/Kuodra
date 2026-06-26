package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Category
import com.arenacun.kuodra.domain.model.Person
import com.arenacun.kuodra.domain.model.UseCase

/** Datos agregados del dashboard: personas (deudas/aportes) y desglose por categoría. */
interface SummaryRepository {
    /** Personas del dashboard (gastos/caja); vacío en Personal. */
    fun people(useCase: UseCase): List<Person>

    /** Desglose por categoría (dashboard personal). */
    fun categories(): List<Category>
}
