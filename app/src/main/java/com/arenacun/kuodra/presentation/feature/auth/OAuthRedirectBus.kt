package com.arenacun.kuodra.presentation.feature.auth

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Código + estado recibidos en el redirect de OAuth2 (`?code=…&state=…`). */
data class OAuthCallback(val code: String, val state: String)

/**
 * Puente entre `MainActivity` (que recibe el deeplink del App Link de OAuth2) y la pantalla de
 * auth (que completa el canje). Singleton de proceso: `MainActivity` publica el callback y la
 * UI lo colecta. `replay = 1` cubre el arranque en frío (el link puede abrir la app estando el
 * proceso muerto); la UI llama a [clear] tras consumirlo para no re-disparar en recomposición.
 */
class OAuthRedirectBus {
    private val _callbacks = MutableSharedFlow<OAuthCallback>(replay = 1, extraBufferCapacity = 1)
    val callbacks: Flow<OAuthCallback> = _callbacks

    fun publish(code: String, state: String) {
        _callbacks.tryEmit(OAuthCallback(code, state))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun clear() {
        _callbacks.resetReplayCache()
    }
}
