package pl.gi.codingchallenge.shared.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import pl.gi.codingchallenge.shared.remote.dto.GitHubSearchResponse
import pl.gi.codingchallenge.shared.remote.dto.RepositoryDto
import pl.gi.codingchallenge.shared.remote.dto.UserDto

private const val GITHUB_BASE_URL = "https://api.github.com"

// Same shape as the deleted CatFactApi: takes an engine, not a client/factory - the
// one seam that makes commonTest MockEngine substitution possible, and the one every
// platform module already provides a binding for (androidPlatformModule,
// iosPlatformModule). Replaces :app's Retrofit-based GitHubApi, which can't run on
// Kotlin/Native.
class GitHubApi(engine: HttpClientEngine) {

    private val client = HttpClient(engine) {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun searchRepositories(query: String, perPage: Int): GitHubSearchResponse<RepositoryDto> =
        client.get("$GITHUB_BASE_URL/search/repositories") {
            parameter("q", query)
            parameter("per_page", perPage)
        }.body()

    suspend fun searchUsers(query: String, perPage: Int): GitHubSearchResponse<UserDto> =
        client.get("$GITHUB_BASE_URL/search/users") {
            parameter("q", query)
            parameter("per_page", perPage)
        }.body()
}
