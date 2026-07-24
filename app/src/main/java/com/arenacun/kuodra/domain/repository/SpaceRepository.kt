package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Space
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Espacio activo + gestión de los espacios de Gastos (multi-instancia, archivables). Personal es
 * único y sintético ([Space.PERSONAL]); no aparece en [spaces].
 */
interface SpaceRepository {
    val activeSpace: StateFlow<Space>

    /** Espacios de Gastos del usuario (incluye los archivados; la UI los separa). */
    val spaces: Flow<List<Space>>

    /** Activa el espacio Personal. */
    fun selectPersonal()

    /** Activa un espacio de Gastos por id. */
    fun selectSpace(id: String)

    /** Crea un espacio de Gastos y lo deja activo. Devuelve el espacio creado. */
    suspend fun createSpace(name: String): Space

    suspend fun rename(id: String, name: String)
    suspend fun setReminder(id: String, enabled: Boolean)
    suspend fun archive(id: String)
    suspend fun unarchive(id: String)

    /** `true` si el usuario ya completó el onboarding (eligió un modo al menos una vez). */
    suspend fun isConfigured(): Boolean
}
