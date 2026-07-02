package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.data.local.SessionStore
import com.arenacun.kuodra.data.remote.AuthApi
import com.arenacun.kuodra.data.sync.SyncTrigger
import com.arenacun.kuodra.domain.model.Session
import com.arenacun.kuodra.domain.repository.AuthRepository
import com.arenacun.kuodra.domain.telemetry.LogLevel
import com.arenacun.kuodra.domain.telemetry.NoOpTelemetry
import com.arenacun.kuodra.domain.telemetry.Telemetry
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
    private val telemetry: Telemetry = NoOpTelemetry,
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
        telemetry.setUser(auth.record.id, auth.record.email)
        telemetry.breadcrumb("auth", "login ok (OTP)")
        telemetry.flush() // sube lo pendiente (incluye eventos capturados antes del login)
        syncTrigger.requestSync() // trae el respaldo del usuario tras iniciar sesión
    }

    override suspend fun restoreSession(): Session? {
        val token = sessionStore.token()
        telemetry.breadcrumb("auth", "restoreSession", mapOf("hasToken" to (token != null).toString()))
        if (token == null) {
            telemetry.log(LogLevel.Warning, "restoreSession: sin token en DataStore")
            return null
        }
        val cached = sessionStore.sessionFlow.first()
        return try {
            val auth = authApi.authRefresh(token)
            sessionStore.save(auth.token, auth.record.id, auth.record.email, auth.record.name)
            telemetry.setUser(auth.record.id, auth.record.email)
            telemetry.breadcrumb("auth", "restoreSession: token válido")
            telemetry.flush()
            syncTrigger.requestSync() // sincroniza al reabrir con sesión válida
            Session(auth.record.id, auth.record.email, auth.record.name)
        } catch (e: ClientRequestException) {
            // 4xx (token inválido/expirado): forzamos re-login.
            telemetry.log(
                LogLevel.Warning, "restoreSession: token rechazado, cierro sesión",
                tags = mapOf("status" to e.response.status.value.toString()),
            )
            sessionStore.clear()
            null
        } catch (e: Exception) {
            // Sin red u otro fallo transitorio: conservamos la sesión local.
            telemetry.breadcrumb("auth", "restoreSession: error transitorio, conservo sesión",
                mapOf("error" to (e::class.simpleName ?: "?")))
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
        telemetry.breadcrumb("auth", "signOut")
        telemetry.setUser(null, null)
    }

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}
