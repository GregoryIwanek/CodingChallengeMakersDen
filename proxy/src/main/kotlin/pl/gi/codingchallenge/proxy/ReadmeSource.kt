package pl.gi.codingchallenge.proxy

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

fun interface ReadmeSource {
    /** The README text for `owner/name`, or null when the repo has none. */
    suspend fun fetch(repo: String): String?
}

/**
 * Uses GitHub's `/readme` endpoint rather than a raw `README.md` URL, so `README.rst`,
 * `readme.md` and other names resolve too. The raw media type returns the file itself
 * instead of base64-wrapped JSON.
 */
class GitHubReadmeSource(private val http: HttpClient) : ReadmeSource {
    override suspend fun fetch(repo: String): String? {
        val response =
            http.get("https://api.github.com/repos/$repo/readme") {
                header(HttpHeaders.Accept, "application/vnd.github.raw+json")
            }
        return when {
            response.status == HttpStatusCode.NotFound -> null
            response.status.isSuccess() -> response.bodyAsText()
            else -> error("GitHub returned ${response.status} for $repo")
        }
    }
}
