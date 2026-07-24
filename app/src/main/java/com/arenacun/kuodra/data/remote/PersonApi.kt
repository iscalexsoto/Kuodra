package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.PbListResponse
import com.arenacun.kuodra.data.remote.dto.PersonDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Endpoints de la colección `persons` en PocketBase. Interfaz para *fakear* en tests. */
interface PersonApi {
    suspend fun list(since: String, token: String): List<PersonDto>
    suspend fun create(dto: PersonDto, token: String): PersonDto
    suspend fun update(dto: PersonDto, token: String): PersonDto
}

class KtorPersonApi(private val client: PocketBaseClient) : PersonApi {

    private val url = client.records("persons")

    override suspend fun list(since: String, token: String): List<PersonDto> {
        val all = mutableListOf<PersonDto>()
        var page = 1
        while (true) {
            val response: PbListResponse<PersonDto> = client.http.get(url) {
                pocketBaseAuth(token)
                pbListParams(since, page)
            }.body()
            all += response.items
            if (page >= response.totalPages || response.items.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun create(dto: PersonDto, token: String): PersonDto =
        client.http.post(url) {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()

    override suspend fun update(dto: PersonDto, token: String): PersonDto =
        client.http.patch("$url/${dto.id}") {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()
}
