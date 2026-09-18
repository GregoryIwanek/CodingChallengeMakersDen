package pl.gi.codingchallenge.shared.remote

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

private const val VALID_USER: String = """
    {"total_count":1,"incomplete_results":false,"items":[
      {"id":2,"login":"octocat","avatar_url":"https://a.co/u.png",
       "html_url":"https://github.com/octocat","type":"User","score":1.0}
    ]}
"""

private const val VALID_REPO: String = """
    {"total_count":1,"incomplete_results":false,"items":[
      {"id":1,"name":"cool-repo","full_name":"octocat/cool-repo",
       "owner":{"id":2,"login":"octocat","avatar_url":"https://a.co/u.png",
                "html_url":"https://github.com/octocat","type":"User"},
       "description":"A cool repo","stargazers_count":42,"score":1.0}
    ]}
"""

private const val ERROR_BODY: String = """{"message":"boom"}"""

// Real bug this locks in (docs/backlog.md AT-12): a plain coroutineScope cancels the sibling
// async when one throws, discarding real, already-fetched results. Routes MockEngine's
// response by request path so users/repos can succeed or fail independently - the real
// GitHubApi/Ktor integration boundary, not a hand-rolled fake standing in for it.
@OptIn(ExperimentalCoroutinesApi::class)
class GitHubSearchNetworkSourceImplTest {

    private fun engineWith(
        usersStatus: HttpStatusCode = HttpStatusCode.OK,
        usersBody: String = VALID_USER,
        reposStatus: HttpStatusCode = HttpStatusCode.OK,
        reposBody: String = VALID_REPO,
        reposDelayMs: Long = 0
    ): MockEngine = MockEngine { request ->
        when (request.url.encodedPath) {
            "/search/users" -> respond(
                usersBody,
                usersStatus,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
            "/search/repositories" -> {
                if (reposDelayMs > 0) delay(reposDelayMs)
                respond(
                    reposBody,
                    reposStatus,
                    headersOf(HttpHeaders.ContentType, "application/json")
                )
            }
            else -> error("unexpected path ${request.url.encodedPath}")
        }
    }

    @Test
    fun repositoriesFailingStillReturnsUsersResults() = runTest {
        val source: GitHubSearchNetworkSourceImpl = GitHubSearchNetworkSourceImpl(
            GitHubApi(
                engineWith(reposStatus = HttpStatusCode.InternalServerError, reposBody = ERROR_BODY)
            )
        )

        val result: List<SearchResultItem> = source.search(query = "kot", perTypeLimit = 10)

        assertEquals(1, result.size)
        assertIs<SearchResultItem.UserResult>(result.single())
    }

    @Test
    fun usersFailingStillReturnsRepositoriesResults() = runTest {
        val source: GitHubSearchNetworkSourceImpl = GitHubSearchNetworkSourceImpl(
            GitHubApi(
                engineWith(usersStatus = HttpStatusCode.InternalServerError, usersBody = ERROR_BODY)
            )
        )

        val result: List<SearchResultItem> = source.search(query = "kot", perTypeLimit = 10)

        assertEquals(1, result.size)
        assertIs<SearchResultItem.RepoResult>(result.single())
    }

    @Test
    fun bothFailingThrowsInsteadOfReturningAnEmptyList() = runTest {
        val source: GitHubSearchNetworkSourceImpl = GitHubSearchNetworkSourceImpl(
            GitHubApi(
                engineWith(
                    usersStatus = HttpStatusCode.InternalServerError,
                    usersBody = ERROR_BODY,
                    reposStatus = HttpStatusCode.InternalServerError,
                    reposBody = ERROR_BODY
                )
            )
        )

        assertFailsWith<GitHubApiException.ServerError> {
            source.search(query = "kot", perTypeLimit = 10)
        }
    }

    // Confirms the observable behavior: cancelling mid-search propagates CancellationException,
    // not a partial-success result. Checked empirically that this test does NOT actually
    // discriminate between catchingCancellationAware() and a plain runCatching here - a
    // cancelled Deferred's .await() throws regardless of what its coroutine body caught and
    // returned, because Job cancellation state is tracked independently of the returned value.
    // See catchingCancellationAware()'s own comment for why it's kept anyway.
    @Test
    fun cancellationWhileOneCallIsInFlightPropagatesInsteadOfReturningPartialResults() = runTest {
        val source: GitHubSearchNetworkSourceImpl = GitHubSearchNetworkSourceImpl(GitHubApi(engineWith(reposDelayMs = 1_000)))

        var caught: Throwable? = null
        val job: kotlinx.coroutines.Job = launch {
            try {
                source.search(query = "kot", perTypeLimit = 10)
            } catch (e: Throwable) {
                caught = e
                throw e
            }
        }
        runCurrent() // let users complete and repos reach its delay
        job.cancel()
        job.join()

        assertIs<CancellationException>(caught)
    }
}
