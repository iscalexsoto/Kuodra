package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.domain.repository.AuthRepository

/**
 * Stub de autenticación del prototipo: el código válido es `123456`.
 * Cuando exista backend, `requestOtp`/`verifyOtp` pasan por `data/remote`.
 */
class AuthRepositoryImpl : AuthRepository {

    override fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    override suspend fun requestOtp(email: String) {
        // Maqueta: no envía nada; el código demo siempre es 123456.
    }

    override suspend fun verifyOtp(code: String): Result<Unit> =
        if (code == DEMO_CODE) Result.success(Unit)
        else Result.failure(IllegalArgumentException("Código incorrecto"))

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        const val DEMO_CODE = "123456"
    }
}
