package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Category

/** Datos agregados del dashboard: desglose por categoría. */
interface SummaryRepository {
    /** Desglose por categoría (dashboard personal). */
    fun categories(): List<Category>
}
