package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.PbListResponse
import com.arenacun.kuodra.data.remote.dto.PeriodSnapshotDto
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Endpoints de la colección `period_snapshots` en PocketBase. */
interface PeriodSnapshotApi {
    suspend fun list(since: String, token: String): List<PeriodSnapshotDto>
    suspend fun create(dto: PeriodSnapshotDto, token: String): PeriodSnapshotDto
    suspend fun update(dto: PeriodSnapshotDto, token: String): PeriodSnapshotDto
}

class KtorPeriodSnapshotApi(private val client: PocketBaseClient) : PeriodSnapshotApi {

    private val url = client.records("period_snapshots")

    override suspend fun list(since: String, token: String): List<PeriodSnapshotDto> {
        val all = mutableListOf<PeriodSnapshotDto>()
        var page = 1
        while (true) {
            val response: PbListResponse<PeriodSnapshotDto> = client.http.get(url) {
                pocketBaseAuth(token)
                pbListParams(since, page)
            }.body()
            all += response.items
            if (page >= response.totalPages || response.items.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun create(dto: PeriodSnapshotDto, token: String): PeriodSnapshotDto =
        client.http.post(url) {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()

    override suspend fun update(dto: PeriodSnapshotDto, token: String): PeriodSnapshotDto =
        client.http.patch("$url/${dto.id}") {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()
}
