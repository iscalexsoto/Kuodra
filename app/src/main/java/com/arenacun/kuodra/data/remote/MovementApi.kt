package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.MovementDto
import com.arenacun.kuodra.data.remote.dto.PbListResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Endpoints de la colección `movements` en PocketBase. Interfaz para poder *fakear* en tests; la
 * impl Ktor lanza excepciones (expectSuccess) que el [com.arenacun.kuodra.data.sync.SyncManager]
 * traduce a `Result`.
 */
interface MovementApi {
    /** Registros del usuario con `updated > since` (vacío = todos), incluyendo tombstones. */
    suspend fun list(since: String, token: String): List<MovementDto>
    suspend fun create(dto: MovementDto, token: String): MovementDto
    suspend fun update(dto: MovementDto, token: String): MovementDto
}

class KtorMovementApi(private val client: PocketBaseClient) : MovementApi {

    private val url = client.records("movements")

    override suspend fun list(since: String, token: String): List<MovementDto> {
        val all = mutableListOf<MovementDto>()
        var page = 1
        while (true) {
            val response: PbListResponse<MovementDto> = client.http.get(url) {
                pocketBaseAuth(token)
                pbListParams(since, page)
            }.body()
            all += response.items
            if (page >= response.totalPages || response.items.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun create(dto: MovementDto, token: String): MovementDto =
        client.http.post(url) {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()

    override suspend fun update(dto: MovementDto, token: String): MovementDto =
        client.http.patch("$url/${dto.id}") {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()
}
