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
}
