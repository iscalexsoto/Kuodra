package com.arenacun.kuodra.presentation.navigation

import com.arenacun.kuodra.domain.model.UseCase
import kotlinx.serialization.Serializable

/**
 * Rutas type-safe de Navigation-Compose. Cada destino es un tipo `@Serializable`;
 * los que llevan argumentos los declaran como propiedades.
 */
sealed interface Destination {

    /** Grafo anidado del flujo de auth; permite compartir el AuthViewModel entre sus pantallas. */
    @Serializable data object AuthGraph : Destination

    // --- Auth ---
    @Serializable data object Welcome : Destination
    @Serializable data object Email : Destination
    @Serializable data object Otp : Destination

    // --- Onboarding ---
    @Serializable data object Name : Destination
    @Serializable data object Mode : Destination
    @Serializable data class CreateSpace(val useCase: UseCase) : Destination

    // --- App ---
    @Serializable data object Dashboard : Destination
    @Serializable data class MovementDetail(val id: String) : Destination
    @Serializable data object AddMovement : Destination
    @Serializable data object AllMovements : Destination
    @Serializable data object Settings : Destination
    @Serializable data object Settle : Destination
    @Serializable data object Replenish : Destination
    @Serializable data object History : Destination
    @Serializable data class HistoryDetail(val id: String) : Destination
}
