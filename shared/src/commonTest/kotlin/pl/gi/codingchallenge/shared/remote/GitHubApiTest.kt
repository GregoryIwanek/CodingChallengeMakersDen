package pl.gi.codingchallenge.shared.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GitHubApiTest {

    // Both fixtures below include real unmodeled fields (node_id, private, language,
    // score, type, ...) on purpose - the real GitHub API sends these and a strict
    // Json rejects them with "Encountered an unknown key" (a real regression this
    // suite didn't catch before it shipped; caught live against the real API
    // instead). ignoreUnknownKeys=true in GitHubApi.kt is what these prove.
    @Test
    fun searchRepositories_hitsTheRightEndpointAndParsesResponse() = runTest {
        var requestedPath = ""
        var requestedQuery = ""
        val engine = MockEngine { request ->
            requestedPath = request.url.encodedPath
            requestedQuery = request.url.encodedQuery
            respond(
                content = """
                    {"total_count":1,"incomplete_results":false,"items":[
                      {"id":1,"node_id":"R_kgDOA","name":"cool-repo","full_name":"octocat/cool-repo",
                       "private":false,"language":"Kotlin",
                       "owner":{"id":2,"node_id":"U_kgDOA","login":"octocat","avatar_url":"https://a.co/u.png",
                                "html_url":"https://github.com/octocat","type":"User"},
                       "description":"A cool repo","stargazers_count":42,"score":1.0}
                    ]}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = GitHubApi(engine).searchRepositories(query = "kot", perPage = 10)

        assertEquals("/search/repositories", requestedPath)
        assertEquals("q=kot&per_page=10", requestedQuery)
        assertEquals(1, result.items.size)
        assertEquals("cool-repo", result.items[0].name)
        assertEquals("octocat/cool-repo", result.items[0].fullName)
        assertEquals("octocat", result.items[0].owner.login)
        assertEquals(42, result.items[0].stargazersCount)
    }

    @Test
    fun searchUsers_hitsTheRightEndpointAndParsesResponse() = runTest {
        var requestedPath = ""
        var requestedQuery = ""
        val engine = MockEngine { request ->
            requestedPath = request.url.encodedPath
            requestedQuery = request.url.encodedQuery
            respond(
                content = """
                    {"total_count":1,"incomplete_results":false,"items":[
                      {"id":2,"node_id":"U_kgDOA","login":"octocat","avatar_url":"https://a.co/u.png",
                       "html_url":"https://github.com/octocat","type":"User","score":1.0}
                    ]}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = GitHubApi(engine).searchUsers(query = "kot", perPage = 10)

        assertEquals("/search/users", requestedPath)
        assertEquals("q=kot&per_page=10", requestedQuery)
        assertEquals(1, result.items.size)
        assertEquals("octocat", result.items[0].login)
        assertEquals("https://github.com/octocat", result.items[0].htmlUrl)
    }

    // These three lock in the fix for the bug docs/architecture/github-api-error-handling.md
    // describes: a non-2xx response used to be deserialized as if it were a successful
    // GitHubSearchResponse, failing with a misleading "Field 'total_count' is required"
    // instead of a clear, typed error.
    @Test
    fun searchRepositories_rateLimited_throwsRateLimitedWithClearMessage() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"message":"API rate limit exceeded for 1.2.3.4 (but here's the good news...)"}""",
                status = HttpStatusCode.Forbidden,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val exception = assertFailsWith<GitHubApiException.RateLimited> {
            GitHubApi(engine).searchRepositories(query = "kot", perPage = 10)
        }
        assertTrue(exception.message!!.contains("rate limit", ignoreCase = true))
    }

    @Test
    fun searchUsers_serverError_throwsServerError() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"message":"Internal Server Error"}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val exception = assertFailsWith<GitHubApiException.ServerError> {
            GitHubApi(engine).searchUsers(query = "kot", perPage = 10)
        }
        assertTrue(exception.message!!.contains("500"))
    }

    @Test
    fun searchRepositories_validationError_throwsUnknownWithStatusAndBody() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"message":"Validation Failed","errors":[{"code":"invalid"}]}""",
                status = HttpStatusCode.UnprocessableEntity,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val exception = assertFailsWith<GitHubApiException.Unknown> {
            GitHubApi(engine).searchRepositories(query = "   ", perPage = 10)
        }
        assertTrue(exception.message!!.contains("422"))
        assertTrue(exception.message!!.contains("Validation Failed"))
    }

    // The four below close gaps the three above left: every named case in
    // GitHubApiException gets its own proof, not just the two the original bug
    // report happened to mention.

    @Test
    fun searchUsers_notFound_throwsNotFound() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"message":"Not Found"}""",
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertFailsWith<GitHubApiException.NotFound> {
            GitHubApi(engine).searchUsers(query = "kot", perPage = 10)
        }
    }

    @Test
    fun searchRepositories_unauthorized_throwsUnauthorized() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"message":"Bad credentials"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertFailsWith<GitHubApiException.Unauthorized> {
            GitHubApi(engine).searchRepositories(query = "kot", perPage = 10)
        }
    }

    // GitHub uses two different status codes for "you're being rate-limited" - 403 for the
    // primary search-API limit (covered above) and 429 for the secondary rate limit its docs
    // describe for other endpoints. Both must map to the same RateLimited case.
    @Test
    fun searchRepositories_tooManyRequests_alsoThrowsRateLimited() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = """{"message":"You have exceeded a secondary rate limit"}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        assertFailsWith<GitHubApiException.RateLimited> {
            GitHubApi(engine).searchRepositories(query = "kot", perPage = 10)
        }
    }

    // A real outage can return plain text or an HTML error page instead of GitHub's normal
    // JSON error body (e.g. a load balancer's own 503 page). bodyOrThrow() reads the error
    // body as plain text, not as JSON, specifically so this doesn't crash a second time on
    // top of the original error - this proves that choice actually holds.
    @Test
    fun searchUsers_nonJsonErrorBody_stillThrowsClearlyInsteadOfCrashingOnParse() = runTest {
        val engine = MockEngine { _ ->
            respond(
                content = "<html><body>503 Service Unavailable</body></html>",
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, "text/html"),
            )
        }

        val exception = assertFailsWith<GitHubApiException.ServerError> {
            GitHubApi(engine).searchUsers(query = "kot", perPage = 10)
        }
        assertTrue(exception.message!!.contains("503"))
    }
}
