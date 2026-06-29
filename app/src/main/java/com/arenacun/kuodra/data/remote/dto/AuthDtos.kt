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

/** Respuesta de auth (`auth-with-otp` / `auth-refresh`): token + registro del usuario. */
@Serializable
data class AuthResponse(
    val token: String,
    val record: UserRecordDto,
)

/** Registro de la colección `users` (solo los campos que consume la app). */
@Serializable
data class UserRecordDto(
    val id: String,
    val email: String = "",
    val name: String = "",
)
