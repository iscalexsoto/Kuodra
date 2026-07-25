package com.arenacun.kuodra.data.repository

import com.arenacun.kuodra.BuildConfig
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
import java.net.URLEncoder

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
    /** Redirect del OAuth2 (App Link HTTPS); debe coincidir con Google console y el manifest. */
    private val oauthRedirectUrl: String = BuildConfig.OAUTH_REDIRECT_URL,
    /** Scope del `stateIn` de la sesión; en tests se pasa `backgroundScope`. */
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : AuthRepository {

    /** `otpId` de la última solicitud; enlaza request-otp con auth-with-otp. */
    private var pendingOtpId: String? = null

    /** Reto PKCE del login OAuth2 en curso; enlaza startGoogleSignIn con completeOAuth2. */
    private var pendingOAuth: PendingOAuth? = null

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

    override suspend fun startGoogleSignIn(): Result<String> = runCatching {
        val provider = authApi.listAuthMethods().oauth2.providers.firstOrNull { it.name == GOOGLE }
            ?: error("El proveedor Google no está habilitado en el servidor.")
        pendingOAuth = PendingOAuth(provider.name, provider.state, provider.codeVerifier)
        // authURL viene firmado con PKCE y termina en `redirect_uri=`; le concatenamos el redirect.
        provider.authURL + URLEncoder.encode(oauthRedirectUrl, "UTF-8")
    }

    override suspend fun completeOAuth2(code: String, state: String): Result<Unit> = runCatching {
        val pending = pendingOAuth ?: error("No hay un inicio de sesión con Google en curso.")
        require(state == pending.state) { "El parámetro state no coincide (posible CSRF)." }
        val auth = authApi.authWithOAuth2(pending.provider, code, pending.codeVerifier, oauthRedirectUrl)
        // El nombre del perfil de Google llega en `meta`; se usa como respaldo si el registro
        // no trae `name` (así el usuario de Google se salta la pantalla de "¿cómo te llamas?").
        val name = auth.record.name.ifBlank { auth.meta?.name.orEmpty() }
        sessionStore.save(auth.token, auth.record.id, auth.record.email, name)
        pendingOAuth = null
        telemetry.setUser(auth.record.id, auth.record.email)
        telemetry.breadcrumb("auth", "login ok (Google)")
        telemetry.flush()
        syncTrigger.requestSync()
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
        pendingOAuth = null
        sessionStore.clear()
        telemetry.breadcrumb("auth", "signOut")
        telemetry.setUser(null, null)
    }

    /** Reto PKCE + estado del proveedor guardados entre el inicio y el canje de OAuth2. */
    private data class PendingOAuth(
        val provider: String,
        val state: String,
        val codeVerifier: String,
    )

    private companion object {
        val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
        const val GOOGLE = "google"
    }
}
