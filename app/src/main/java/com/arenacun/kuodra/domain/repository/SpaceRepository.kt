package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Space
import com.arenacun.kuodra.domain.model.UseCase
import kotlinx.coroutines.flow.StateFlow

/** Mantiene el espacio activo (caso de uso + nombre) de forma observable y persistente. */
interface SpaceRepository {
    val activeSpace: StateFlow<Space>

    /** Cambia el caso de uso conservando el flujo (reinicia el nombre al default). */
    fun selectUseCase(useCase: UseCase)

    /** Crea/configura el espacio con un caso de uso y nombre. */
    fun createSpace(useCase: UseCase, name: String)

    /** `true` si el usuario ya eligió un caso de uso (onboarding completado). */
    suspend fun isConfigured(): Boolean
}
