package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.PbListResponse
import com.arenacun.kuodra.data.remote.dto.SpaceDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Endpoints de la colección `spaces` en PocketBase. Interfaz para *fakear* en tests. */
interface SpaceApi {
    suspend fun list(since: String, token: String): List<SpaceDto>
    suspend fun create(dto: SpaceDto, token: String): SpaceDto
    suspend fun update(dto: SpaceDto, token: String): SpaceDto
}

class KtorSpaceApi(private val client: PocketBaseClient) : SpaceApi {

    private val url = client.records("spaces")

    override suspend fun list(since: String, token: String): List<SpaceDto> {
        val all = mutableListOf<SpaceDto>()
        var page = 1
        while (true) {
            val response: PbListResponse<SpaceDto> = client.http.get(url) {
                pocketBaseAuth(token)
                pbListParams(since, page)
            }.body()
            all += response.items
            if (page >= response.totalPages || response.items.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun create(dto: SpaceDto, token: String): SpaceDto =
        client.http.post(url) {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()

    override suspend fun update(dto: SpaceDto, token: String): SpaceDto =
        client.http.patch("$url/${dto.id}") {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()
}
