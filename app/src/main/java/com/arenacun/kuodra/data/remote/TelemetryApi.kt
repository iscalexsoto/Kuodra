package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.TelemetryEventDto
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Endpoint de la colección `telemetry_events` de PocketBase. Interfaz para poder *fakear* en tests;
 * la impl Ktor lanza (expectSuccess) y el subidor traduce a `Result`.
 */
interface TelemetryApi {
    /** Sube un evento. La colección tiene create rule autenticada, de ahí el `token`. */
    suspend fun send(dto: TelemetryEventDto, token: String)
}

class KtorTelemetryApi(private val client: PocketBaseClient) : TelemetryApi {

    private val url = client.records("telemetry_events")

    override suspend fun send(dto: TelemetryEventDto, token: String) {
        client.http.post(url) {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }
    }
}
