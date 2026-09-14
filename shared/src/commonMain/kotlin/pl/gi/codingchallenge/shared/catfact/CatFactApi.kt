package pl.gi.codingchallenge.shared.catfact

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json

// Takes an engine, not an HttpClientFactory or similar - this is the one seam that
// makes the commonTest MockEngine substitution possible, the same role a constructor
// parameter for a repository interface played in step 1's ConversionHistoryRepository.
// Deliberately a thin class, not an interface+impl pair: there's only one real
// implementation anyone would plausibly want (a real HTTP call), and the engine is
// already the injection point for tests.
class CatFactApi(engine: HttpClientEngine) {

    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun fetchCatFact(): CatFact =
        client.get("https://catfact.ninja/fact").body()
}
