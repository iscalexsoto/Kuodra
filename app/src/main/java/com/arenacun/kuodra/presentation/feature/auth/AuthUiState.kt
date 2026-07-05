package com.arenacun.kuodra.presentation.feature.auth

data class AuthUiState(
    val email: String = "",
    val emailValid: Boolean = false,
    /** Enviando la solicitud de código (deshabilita el botón / muestra "Enviando…"). */
    val requesting: Boolean = false,
    /** Mensaje de error de la solicitud de código (red/servidor). */
    val requestError: String? = null,
    val otp: String = "",
    /** Verificando el código de 6 dígitos contra el servidor. */
    val verifying: Boolean = false,
    val otpError: Boolean = false,
    /** Login con Google en curso (pidiendo el proveedor o canjeando el código). */
    val googleLoading: Boolean = false,
    /** Mensaje de error del login con Google (red/servidor/cancelación). */
    val authError: String? = null,
)
