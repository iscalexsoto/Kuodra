package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.BudgetDto
import com.arenacun.kuodra.data.remote.dto.PbListResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Endpoints de la colección `budgets` en PocketBase (una fila por usuario). */
interface BudgetApi {
    suspend fun list(since: String, token: String): List<BudgetDto>
    suspend fun create(dto: BudgetDto, token: String): BudgetDto
    suspend fun update(dto: BudgetDto, token: String): BudgetDto
}

class KtorBudgetApi(private val client: PocketBaseClient) : BudgetApi {

    private val url = client.records("budgets")

    override suspend fun list(since: String, token: String): List<BudgetDto> {
        val all = mutableListOf<BudgetDto>()
        var page = 1
        while (true) {
            val response: PbListResponse<BudgetDto> = client.http.get(url) {
                pocketBaseAuth(token)
                pbListParams(since, page)
            }.body()
            all += response.items
            if (page >= response.totalPages || response.items.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun create(dto: BudgetDto, token: String): BudgetDto =
        client.http.post(url) {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()

    override suspend fun update(dto: BudgetDto, token: String): BudgetDto =
        client.http.patch("$url/${dto.id}") {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()
}
