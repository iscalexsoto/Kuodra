package com.arenacun.kuodra.domain.repository

import com.arenacun.kuodra.domain.model.Session
import kotlinx.coroutines.flow.StateFlow

/**
 * Autenticación sin contraseña contra PocketBase. Dos métodos sobre la misma cuenta `users`:
 * - **Correo + OTP:** [requestOtp] (alta-si-no-existe + envío del código) → [verifyOtp].
 * - **Google (OAuth2):** [startGoogleSignIn] (abre el navegador) → [completeOAuth2] (canje del
 *   código del redirect). Ambos persisten la sesión localmente.
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
     * Inicia el login con Google (OAuth2): pide al servidor el proveedor y su reto PKCE, y
     * devuelve la URL de autorización a abrir en el navegador. El `state`/`codeVerifier` quedan
     * guardados para el canje posterior en [completeOAuth2].
     * Proveedor no habilitado o error de red/servidor ⇒ [Result.failure].
     */
    suspend fun startGoogleSignIn(): Result<String>

    /**
     * Completa el login OAuth2 con el `code` y `state` recibidos en el redirect. Valida el
     * `state` (anti-CSRF), canjea el código por una sesión y la persiste localmente.
     * `state` no coincidente, sin flujo en curso o error ⇒ [Result.failure].
     */
    suspend fun completeOAuth2(code: String, state: String): Result<Unit>

    /**
     * Actualiza el nombre del usuario en PocketBase y en la sesión persistida.
     * Sin sesión activa o error de red/servidor ⇒ [Result.failure].
     */
    suspend fun updateName(name: String): Result<Unit>

    /** Cierra la sesión y limpia el almacenamiento local. */
    suspend fun signOut()
}
