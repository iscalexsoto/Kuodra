package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.AuthResponse
import com.arenacun.kuodra.data.remote.dto.AuthWithOtpRequest
import com.arenacun.kuodra.data.remote.dto.CreateUserRequest
import com.arenacun.kuodra.data.remote.dto.RequestOtpRequest
import com.arenacun.kuodra.data.remote.dto.RequestOtpResponse
import com.arenacun.kuodra.data.remote.dto.UpdateUserRequest
import com.arenacun.kuodra.data.remote.dto.UserRecordDto
import io.ktor.client.call.body
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Endpoints de autenticación de la colección `users` de PocketBase.
 * Interfaz para poder sustituirla por un fake en tests; [KtorAuthApi] es la impl real.
 */
interface AuthApi {
    /** Alta de usuario con contraseña aleatoria (solo entramos por OTP). */
    suspend fun createUser(email: String): UserRecordDto

    /** Solicita el código OTP; devuelve el `otpId` para el canje posterior. */
    suspend fun requestOtp(email: String): RequestOtpResponse

    /** Canjea el `otpId` + código por una sesión (token + registro). */
    suspend fun authWithOtp(otpId: String, code: String): AuthResponse

    /** Refresca y valida el token persistido; 401 ⇒ excepción. */
    suspend fun authRefresh(token: String): AuthResponse

    /** Edita el nombre del registro del usuario; devuelve el registro actualizado. */
    suspend fun updateUser(userId: String, name: String, token: String): UserRecordDto
}

/**
 * Implementación Ktor. No maneja errores: deja propagar las excepciones de Ktor
 * (`expectSuccess`) para que el repositorio decida qué hacer con cada una.
 */
class KtorAuthApi(
    private val client: PocketBaseClient,
) : AuthApi {

    override suspend fun createUser(email: String): UserRecordDto {
        val password = randomPassword()
        return client.http.post(client.collectionUrl("records")) {
            jsonBody()
            setBody(CreateUserRequest(email, password, password))
        }.body()
    }

    override suspend fun requestOtp(email: String): RequestOtpResponse =
        client.http.post(client.collectionUrl("request-otp")) {
            jsonBody()
            setBody(RequestOtpRequest(email))
        }.body()

    override suspend fun authWithOtp(otpId: String, code: String): AuthResponse =
        client.http.post(client.collectionUrl("auth-with-otp")) {
            jsonBody()
            setBody(AuthWithOtpRequest(otpId, code))
        }.body()

    override suspend fun authRefresh(token: String): AuthResponse =
        client.http.post(client.collectionUrl("auth-refresh")) {
            pocketBaseAuth(token)
        }.body()

    override suspend fun updateUser(userId: String, name: String, token: String): UserRecordDto =
        client.http.patch(client.collectionUrl("records/$userId")) {
            jsonBody()
            pocketBaseAuth(token)
            setBody(UpdateUserRequest(name))
        }.body()

    private fun randomPassword(): String {
        val chars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        return (1..24).map { chars.random() }.joinToString("")
    }
}
