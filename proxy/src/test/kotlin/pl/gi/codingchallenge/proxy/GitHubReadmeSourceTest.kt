package pl.gi.codingchallenge.proxy

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** Runs the real [GitHubReadmeSource] against MockEngine, so no request reaches GitHub. */
class GitHubReadmeSourceTest {
    private var lastRequest: HttpRequestData? = null

    private fun sourceAnswering(status: HttpStatusCode, body: String = "") = GitHubReadmeSource(
        HttpClient(
            MockEngine { request ->
                lastRequest = request
                respond(content = body, status = status)
            }
        )
    )

    @Test
    fun `asks GitHub's readme endpoint for the raw file and returns its text`() = runTest {
        val readme = sourceAnswering(status = HttpStatusCode.OK, body = "# OkHttp\nAn HTTP client.")
            .fetch("square/okhttp")

        assertEquals("# OkHttp\nAn HTTP client.", readme)
        val request = assertNotNull(lastRequest)
        assertEquals("https://api.github.com/repos/square/okhttp/readme", request.url.toString())
        assertEquals("application/vnd.github.raw+json", request.headers[HttpHeaders.Accept])
    }

    @Test
    fun `returns null when the repo has no README`() = runTest {
        assertNull(sourceAnswering(status = HttpStatusCode.NotFound).fetch("square/okhttp"))
    }

    @Test
    fun `throws on any other failure, naming the status and repo`() = runTest {
        // e.g. GitHub's unauthenticated rate limit, which answers 403
        val error = assertFailsWith<IllegalStateException> {
            sourceAnswering(status = HttpStatusCode.Forbidden).fetch("square/okhttp")
        }

        val message = error.message.orEmpty()
        assertTrue("403" in message && "square/okhttp" in message, message)
    }
}
