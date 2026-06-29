package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Session
import kotlinx.coroutines.flow.StateFlow

/**
 * Autenticación sin contraseña (correo + código de un solo uso) contra PocketBase.
 * El flujo OTP es de dos pasos: [requestOtp] (alta-si-no-existe + envío del código) y
 * [verifyOtp] (canje del código por una sesión que se persiste localmente).
 */
interface AuthRepository {
    /** Sesión activa observable; `null` cuando no hay usuario autenticado. */
    val session: StateFlow<Session?>

    /** Valida el formato del correo. */
    fun isValidEmail(email: String): Boolean

    /**
     * Da de alta al usuario si no existe y solicita el envío del código al correo.
     * Éxito ⇒ [Result.success]; error de red/servidor ⇒ [Result.failure].
     */
    suspend fun requestOtp(email: String): Result<Unit>

    /**
     * Verifica el código del último [requestOtp] y persiste la sesión.
     * Código inválido o error ⇒ [Result.failure].
     */
    suspend fun verifyOtp(code: String): Result<Unit>

    /**
     * Restaura y valida la sesión persistida al arrancar la app.
     * Devuelve la [Session] vigente o `null` si no hay/expiró.
     */
    suspend fun restoreSession(): Session?

    /**
     * Actualiza el nombre del usuario en PocketBase y en la sesión persistida.
     * Sin sesión activa o error de red/servidor ⇒ [Result.failure].
     */
    suspend fun updateName(name: String): Result<Unit>

    /** Cierra la sesión y limpia el almacenamiento local. */
    suspend fun signOut()
}
