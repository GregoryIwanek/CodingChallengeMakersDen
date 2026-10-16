package pl.gi.codingchallenge.shared.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.parameter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

class LlmClientImpl(engine: HttpClientEngine, private val baseUrl: String) : LlmClient {

    private val client: HttpClient = HttpClient(engine) { install(SSE) }

    override val isEnabled: Boolean get() = baseUrl.isNotBlank()

    override fun explainRepo(fullName: String): Flow<LlmChunk> = channelFlow {
        client.sse(
            urlString = "$baseUrl/explain",
            request = { parameter("repo", fullName) }
        ) {
            incoming.collect { frame ->
                toLlmChunk(event = frame.event, data = frame.data)?.let { send(it) }
            }
        }
    }
}
