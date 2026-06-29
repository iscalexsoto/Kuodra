package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Category
import kotlinx.coroutines.flow.StateFlow

/**
 * Catálogo de categorías del usuario. Se expone como [StateFlow] para lectura síncrona
 * (`.value`) y reactiva; respaldado por Room (fuente de verdad) y, en Fase 2, sincronizado.
 */
interface CategoryRepository {
    val categories: StateFlow<List<Category>>

    suspend fun add(category: Category)
    suspend fun update(category: Category)
    suspend fun delete(id: String)
}
