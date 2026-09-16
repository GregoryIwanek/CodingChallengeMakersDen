package pl.gi.codingchallenge.shared.domain.repository

import kotlinx.coroutines.test.runTest
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Depends on GitHubSearchNetworkSource/GitHubSearchCache directly - no Ktor, no
// SQLite/SQLDelight driver anywhere in this test. This is the concrete payoff of
// depending on the narrower interfaces instead of the concrete
// GitHubSearchNetworkSourceImpl/SearchResultCache classes. Fakes, not a mocking
// framework (MockK is JVM-only, can't compile in commonTest).
class CachingGitHubSearchRepositoryTest {

    private val network = FakeNetworkSource()
    private val cache = FakeCache()
    private val repository = CachingGitHubSearchRepository(network = network, cache = cache)

    private val results: List<SearchResultItem> = listOf(
        SearchResultItem.RepoResult(
            id = "1", name = "kotlin", fullName = "JetBrains/kotlin",
            ownerLogin = "JetBrains", avatarUrl = null, description = null, stars = 50_000,
        ),
    )

    @Test
    fun searchSuccessFetchesFromNetworkAndCachesTheResult() = runTest {
        network.results = results

        val actual = repository.search(query = "kot", perTypeLimit = 50)

        assertEquals(results, actual)
        assertEquals(listOf("kot" to results), cache.putCalls)
    }

    @Test
    fun searchFailureWithACachedEntryFallsBackToTheCacheNoCrash() = runTest {
        network.error = RuntimeException("offline")
        cache.stored["kot"] = results

        val actual = repository.search(query = "kot", perTypeLimit = 50)

        assertEquals(results, actual)
    }

    @Test
    fun searchFailureWithNoCachedEntryRethrowsTheOriginalException() = runTest {
        network.error = RuntimeException("offline")

        assertFailsWith<RuntimeException> {
            repository.search(query = "kot", perTypeLimit = 50)
        }
    }

    private class FakeNetworkSource : GitHubSearchNetworkSource {
        var results: List<SearchResultItem> = emptyList()
        var error: Throwable? = null

        override suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem> {
            error?.let { throw it }
            return results
        }
    }

    private class FakeCache : GitHubSearchCache {
        val stored = mutableMapOf<String, List<SearchResultItem>>()
        val putCalls = mutableListOf<Pair<String, List<SearchResultItem>>>()

        override suspend fun get(query: String): List<SearchResultItem>? = stored[query]

        override suspend fun put(query: String, results: List<SearchResultItem>) {
            stored[query] = results
            putCalls.add(query to results)
        }
    }
}
