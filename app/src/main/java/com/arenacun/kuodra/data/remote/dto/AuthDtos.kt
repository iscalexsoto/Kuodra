package com.arenacun.kuodra.data.remote.dto

import kotlinx.serialization.Serializable

/** Cuerpo de `POST /api/collections/users/records` (alta de usuario). */
@Serializable
data class CreateUserRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String,
)

/** Cuerpo de `POST /api/collections/users/request-otp`. */
@Serializable
data class RequestOtpRequest(val email: String)

/** Respuesta de `request-otp`: id que enlaza la solicitud con el canje. */
@Serializable
data class RequestOtpResponse(val otpId: String)

/** Cuerpo de `PATCH /api/collections/users/records/{id}` (editar el nombre del usuario). */
@Serializable
data class UpdateUserRequest(val name: String)

/** Cuerpo de `POST /api/collections/users/auth-with-otp` (`password` = código OTP). */
@Serializable
data class AuthWithOtpRequest(
    val otpId: String,
    val password: String,
)

/**
 * Respuesta de auth (`auth-with-otp` / `auth-refresh` / `auth-with-oauth2`): token + registro.
 * En OAuth2 PocketBase añade `meta` con el perfil del proveedor (nombre, correo, avatar); es
 * `null` en el resto de flujos.
 */
@Serializable
data class AuthResponse(
    val token: String,
    val record: UserRecordDto,
    val meta: OAuthMeta? = null,
)

/** Perfil del proveedor OAuth2 devuelto en `meta` (solo en `auth-with-oauth2`). */
@Serializable
data class OAuthMeta(
    val name: String = "",
    val email: String = "",
    val avatarURL: String = "",
)

/** Respuesta de `GET /api/collections/users/auth-methods` (proveedores OAuth2 habilitados). */
@Serializable
data class AuthMethodsResponse(
    val oauth2: OAuth2Config = OAuth2Config(),
)

@Serializable
data class OAuth2Config(
    val enabled: Boolean = false,
    val providers: List<OAuth2ProviderInfo> = emptyList(),
)

/**
 * Proveedor OAuth2 configurado en el servidor. `authURL` es la URL de autorización de Google
 * ya firmada con PKCE; termina en `redirect_uri=` para que el cliente le concatene su redirect.
 * `state` y `codeVerifier` deben conservarse para el canje posterior (anti-CSRF + PKCE).
 */
@Serializable
data class OAuth2ProviderInfo(
    val name: String,
    val state: String,
    val codeVerifier: String,
    val codeChallenge: String = "",
    val codeChallengeMethod: String = "",
    val authURL: String,
)

/** Cuerpo de `POST /api/collections/users/auth-with-oauth2` (canje del código por sesión). */
@Serializable
data class AuthWithOAuth2Request(
    val provider: String,
    val code: String,
    val codeVerifier: String,
    val redirectURL: String,
)

/** Registro de la colección `users` (solo los campos que consume la app). */
@Serializable
data class UserRecordDto(
    val id: String,
    val email: String = "",
    val name: String = "",
)
