package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Cliente HTTP hacia PocketBase. Centraliza el [HttpClient] Ktor (engine OkHttp,
 * JSON tolerante a campos extra) y la construcción de URLs a partir de la base.
 * `expectSuccess = true` ⇒ las respuestas no-2xx lanzan excepción, que los
 * repositorios traducen a `Result.failure`.
 */
class PocketBaseClient(
    val baseUrl: String = BuildConfig.POCKETBASE_URL,
) {
    val http: HttpClient = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.INFO else LogLevel.NONE
        }
    }

    /** URL absoluta de una colección de la API de records. */
    fun collectionUrl(path: String): String =
        "${baseUrl.trimEnd('/')}/api/collections/users/$path"

    /** URL base de records de una colección arbitraria (movements, categories, …). */
    fun records(collection: String): String =
        "${baseUrl.trimEnd('/')}/api/collections/$collection/records"
}

/** Header de autenticación de PocketBase: el token se envía crudo en `Authorization`. */
fun HttpRequestBuilder.pocketBaseAuth(token: String) {
    headers.append("Authorization", token)
}

/** Helper para fijar `Content-Type: application/json` en las peticiones con cuerpo. */
fun HttpRequestBuilder.jsonBody() {
    contentType(ContentType.Application.Json)
}

/** Parámetros de listado de records: paginación, orden por `updated` y filtro de deltas. */
fun HttpRequestBuilder.pbListParams(since: String, page: Int) {
    url {
        parameters.append("perPage", "200")
        parameters.append("sort", "updated")
        parameters.append("page", page.toString())
        if (since.isNotEmpty()) parameters.append("filter", "updated > \"$since\"")
    }
}
