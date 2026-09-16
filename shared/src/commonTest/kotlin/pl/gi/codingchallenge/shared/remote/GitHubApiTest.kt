package pl.gi.codingchallenge.shared.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GitHubApiTest {

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
                      {"id":1,"name":"cool-repo","full_name":"octocat/cool-repo",
                       "owner":{"id":2,"login":"octocat","avatar_url":"https://a.co/u.png",
                                "html_url":"https://github.com/octocat"},
                       "description":"A cool repo","stargazers_count":42}
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
                      {"id":2,"login":"octocat","avatar_url":"https://a.co/u.png",
                       "html_url":"https://github.com/octocat"}
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
}
