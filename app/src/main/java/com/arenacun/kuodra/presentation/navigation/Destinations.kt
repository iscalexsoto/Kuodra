package com.arenacun.kuodra.presentation.navigation

import com.arenacun.kuodra.domain.model.UseCase
import com.arenacun.kuodra.domain.scan.ScanSource
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

    /** Grafo anidado del alta de movimiento; comparte el ScanDraftViewModel entre Scan y Add. */
    @Serializable data object AddGraph : Destination
    /** Alta de movimiento; con [editId] carga un movimiento existente y guarda como edición. */
    @Serializable data class AddMovement(val editId: String? = null) : Destination
    /** Pagadores + división de un gasto compartido (dentro del AddGraph; comparte el AddMovementViewModel). */
    @Serializable data object SplitConfig : Destination
    @Serializable data class ScanTicket(val source: ScanSource) : Destination
    @Serializable data object AllMovements : Destination
    @Serializable data object Settings : Destination
    @Serializable data object Categories : Destination
    @Serializable data object Settle : Destination
    @Serializable data object History : Destination
    @Serializable data class HistoryDetail(val id: String) : Destination
}
