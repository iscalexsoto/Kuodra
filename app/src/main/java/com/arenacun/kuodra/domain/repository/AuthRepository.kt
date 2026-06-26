package com.arenacun.kuodra.domain.repository

/**
 * Autenticación sin contraseña (correo + código de un solo uso).
 * Implementación actual: stub en memoria con la regla demo del prototipo.
 */
interface AuthRepository {
    /** Valida el formato del correo. */
    fun isValidEmail(email: String): Boolean

    /** Solicita el envío del código de 6 dígitos al correo dado. */
    suspend fun requestOtp(email: String)

    /** Verifica el código. Éxito ⇒ [Result.success]; código inválido ⇒ [Result.failure]. */
    suspend fun verifyOtp(code: String): Result<Unit>
}
