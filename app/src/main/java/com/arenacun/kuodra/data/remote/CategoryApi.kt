package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.CategoryDto
import com.arenacun.kuodra.data.remote.dto.PbListResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/** Endpoints de la colección `categories` en PocketBase. */
interface CategoryApi {
    suspend fun list(since: String, token: String): List<CategoryDto>
    suspend fun create(dto: CategoryDto, token: String): CategoryDto
    suspend fun update(dto: CategoryDto, token: String): CategoryDto
}

class KtorCategoryApi(private val client: PocketBaseClient) : CategoryApi {

    private val url = client.records("categories")

    override suspend fun list(since: String, token: String): List<CategoryDto> {
        val all = mutableListOf<CategoryDto>()
        var page = 1
        while (true) {
            val response: PbListResponse<CategoryDto> = client.http.get(url) {
                pocketBaseAuth(token)
                pbListParams(since, page)
            }.body()
            all += response.items
            if (page >= response.totalPages || response.items.isEmpty()) break
            page++
        }
        return all
    }

    override suspend fun create(dto: CategoryDto, token: String): CategoryDto =
        client.http.post(url) {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()

    override suspend fun update(dto: CategoryDto, token: String): CategoryDto =
        client.http.patch("$url/${dto.id}") {
            pocketBaseAuth(token)
            jsonBody()
            setBody(dto)
        }.body()
}
