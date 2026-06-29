package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.remote.AuthApi
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.Session
import com.arenacun.kuodra.domain.repository.AuthRepository
import io.ktor.client.plugins.ClientRequestException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

/**
 * Autenticación contra PocketBase (correo + OTP).
 *
 * `requestOtp` da de alta al usuario si no existe (PocketBase solo envía el código a
 * registros existentes) y guarda el `otpId` para que `verifyOtp` lo canjee por una
 * sesión, que se persiste en [SessionStore].
 */
class AuthRepositoryImpl(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
    private val syncTrigger: SyncTrigger = SyncTrigger.NoOp,
) : AuthRepository {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** `otpId` de la última solicitud; enlaza request-otp con auth-with-otp. */
    private var pendingOtpId: String? = null

    override val session: StateFlow<Session?> =
        sessionStore.sessionFlow.stateIn(scope, SharingStarted.Eagerly, null)

    override fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    override suspend fun requestOtp(email: String): Result<Unit> = runCatching {
        val clean = email.trim()
        // Alta-si-no-existe: si ya existe, PocketBase responde 400 (correo duplicado),
        // que ignoramos para continuar con el envío del código.
        runCatching { authApi.createUser(clean) }
        pendingOtpId = authApi.requestOtp(clean).otpId
    }

    override suspend fun verifyOtp(code: String): Result<Unit> = runCatching {
        val otpId = pendingOtpId ?: error("No hay una solicitud de código activa.")
        val auth = authApi.authWithOtp(otpId, code.trim())
        sessionStore.save(auth.token, auth.record.id, auth.record.email, auth.record.name)
        pendingOtpId = null
        syncTrigger.requestSync() // trae el respaldo del usuario tras iniciar sesión
    }

    override suspend fun restoreSession(): Session? {
        val token = sessionStore.token() ?: return null
        val cached = sessionStore.sessionFlow.first()
        return try {
            val auth = authApi.authRefresh(token)
            sessionStore.save(auth.token, auth.record.id, auth.record.email, auth.record.name)
            syncTrigger.requestSync() // sincroniza al reabrir con sesión válida
            Session(auth.record.id, auth.record.email, auth.record.name)
        } catch (_: ClientRequestException) {
            // 4xx (token inválido/expirado): forzamos re-login.
            sessionStore.clear()
            null
        } catch (_: Exception) {
            // Sin red u otro fallo transitorio: conservamos la sesión local.
            cached
        }
    }

    override suspend fun updateName(name: String): Result<Unit> = runCatching {
        val token = sessionStore.token() ?: error("No hay una sesión activa.")
        val current = sessionStore.sessionFlow.first() ?: error("No hay una sesión activa.")
        val clean = name.trim()
        authApi.updateUser(current.userId, clean, token)
        sessionStore.save(token, current.userId, current.email, clean)
    }

    override suspend fun signOut() {
        pendingOtpId = null
        sessionStore.clear()
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
