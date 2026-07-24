package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.PbListResponse
import com.arenacun.kuodra.data.remote.dto.SettlementDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Endpoints de la colección `settlements` en PocketBase. Interfaz para *fakear* en tests. */
interface SettlementApi {
    suspend fun list(since: String, token: String): List<SettlementDto>
    suspend fun create(dto: SettlementDto, token: String): SettlementDto
    suspend fun update(dto: SettlementDto, token: String): SettlementDto
}

class KtorSettlementApi(private val client: PocketBaseClient) : SettlementApi {

    private val url = client.records("settlements")

    override suspend fun list(since: String, token: String): List<SettlementDto> {
        val all = mutableListOf<SettlementDto>()
        var page = 1
        while (true) {
            val response: PbListResponse<SettlementDto> = client.http.get(url) {
                pocketBaseAuth(token)
                pbListParams(since, page)
            }.body()
            all += response.items
            if (page >= response.totalPages || response.items.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun create(dto: SettlementDto, token: String): SettlementDto =
        client.http.post(url) {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()

    override suspend fun update(dto: SettlementDto, token: String): SettlementDto =
        client.http.patch("$url/${dto.id}") {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()
}
