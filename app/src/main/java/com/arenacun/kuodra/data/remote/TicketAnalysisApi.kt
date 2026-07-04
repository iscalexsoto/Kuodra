package com.arenacun.kuodra.data.remote

import com.arenacun.kuodra.data.remote.dto.TicketAnalysisDto
import com.arenacun.kuodra.data.remote.dto.TicketAnalysisRequest
import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody

/**
 * Análisis remoto de tickets: ruta custom de PocketBase que proxea a Mistral (la API key vive
 * solo en el servidor; el cliente se autentica con su token de sesión). Interfaz fakeable en
 * tests; los fallos (timeout, 401, 502, sin red) los traduce a null el `MistralTicketParser`.
 */
interface TicketAnalysisApi {
    suspend fun analyze(text: String, token: String): TicketAnalysisDto
}

class KtorTicketAnalysisApi(private val client: PocketBaseClient) : TicketAnalysisApi {

    private val url = "${client.baseUrl.trimEnd('/')}/api/kuodra/analyze-ticket"

    override suspend fun analyze(text: String, token: String): TicketAnalysisDto =
        client.http.post(url) {
            pocketBaseAuth(token)
            jsonBody()
            // Acotado: si Mistral tarda, el fallback regex responde en su lugar.
            timeout { requestTimeoutMillis = 15_000 }
            setBody(TicketAnalysisRequest(text))
        }.body()
}
