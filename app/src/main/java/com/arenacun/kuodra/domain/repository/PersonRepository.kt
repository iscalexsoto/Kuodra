package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.SpacePerson
import kotlinx.coroutines.flow.Flow

/** Contactos (Nombre + Teléfono) de un espacio de Gastos. */
interface PersonRepository {
    fun persons(spaceId: String): Flow<List<SpacePerson>>
    suspend fun add(spaceId: String, person: SpacePerson)
    suspend fun update(spaceId: String, person: SpacePerson)
    suspend fun delete(id: String)
}
