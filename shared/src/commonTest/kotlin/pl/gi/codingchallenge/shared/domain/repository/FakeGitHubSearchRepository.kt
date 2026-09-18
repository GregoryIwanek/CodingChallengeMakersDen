package pl.gi.codingchallenge.shared.domain.repository

import kotlinx.coroutines.delay
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

class FakeGitHubSearchRepository : GitHubSearchRepository {

    private val responses: MutableMap<String, Response> = mutableMapOf<String, Response>()

    var searchCallCount: Int = 0
        private set

    override suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem> {
        searchCallCount++
        val response: Response = responses[query] ?: return emptyList()
        if (response.delayMs > 0) delay(response.delayMs)
        response.error?.let { throw it }
        return response.results
    }

    fun enqueueResult(query: String, results: List<SearchResultItem>) {
        responses[query] = Response(results = results)
    }

    fun enqueueDelayedResult(query: String, delayMs: Long, results: List<SearchResultItem>) {
        responses[query] = Response(results = results, delayMs = delayMs)
    }

    fun enqueueError(query: String, error: Throwable) {
        responses[query] = Response(results = emptyList(), error = error)
    }

    fun sampleRepo(name: String): SearchResultItem.RepoResult = SearchResultItem.RepoResult(
        id = name,
        name = name,
        fullName = "owner/$name",
        ownerLogin = "owner",
        avatarUrl = null,
        description = null,
        stars = 0
    )

    private data class Response(
        val results: List<SearchResultItem>,
        val delayMs: Long = 0,
        val error: Throwable? = null
    )
}
