package pl.gi.codingchallenge.shared.domain.repository

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
import pl.gi.codingchallenge.shared.remote.GitHubApiException

// Depends on GitHubSearchNetworkSource/GitHubSearchCache directly - no Ktor, no
// SQLite/SQLDelight driver anywhere in this test. This is the concrete payoff of
// depending on the narrower interfaces instead of the concrete
// GitHubSearchNetworkSourceImpl/SearchResultCache classes. Fakes, not a mocking
// framework (MockK is JVM-only, can't compile in commonTest).
@OptIn(ExperimentalCoroutinesApi::class)
class CachingGitHubSearchRepositoryTest {

    private val network: FakeNetworkSource = FakeNetworkSource()
    private val cache: FakeCache = FakeCache()
    private val repository: CachingGitHubSearchRepository =
        CachingGitHubSearchRepository(network = network, cache = cache)

    private val results: List<SearchResultItem> = listOf(
        SearchResultItem.RepoResult(
            id = "1",
            name = "kotlin",
            fullName = "JetBrains/kotlin",
            ownerLogin = "JetBrains",
            avatarUrl = null,
            description = null,
            stars = 50_000
        )
    )

    @Test
    fun searchSuccessFetchesFromNetworkAndCachesTheResult() = runTest {
        network.results = results

        val actual: List<SearchResultItem> = repository.search(query = "kot", perTypeLimit = 50)

        assertEquals(results, actual)
        assertEquals(listOf("kot" to results), cache.putCalls)
    }

    @Test
    fun searchFailureWithACachedEntryFallsBackToTheCacheNoCrash() = runTest {
        network.error = RuntimeException("offline")
        cache.stored["kot"] = results

        val actual: List<SearchResultItem> = repository.search(query = "kot", perTypeLimit = 50)

        assertEquals(results, actual)
    }

    @Test
    fun searchFailureWithNoCachedEntryRethrowsTheOriginalException() = runTest {
        network.error = RuntimeException("offline")

        assertFailsWith<RuntimeException> {
            repository.search(query = "kot", perTypeLimit = 50)
        }
    }

    // The two tests above use a generic RuntimeException to stand in for "the network call
    // failed, whatever the reason." This one uses the real GitHubApiException type instead -
    // proving the fallback-to-cache path (catch (e: Exception)) genuinely catches this specific
    // sealed subtype too, not just exceptions in general.
    @Test
    fun searchFailureFromGitHubApiExceptionSpecificallyStillFallsBackToCache() = runTest {
        network.error = GitHubApiException.RateLimited(statusCode = 403)
        cache.stored["kot"] = results

        val actual: List<SearchResultItem> = repository.search(query = "kot", perTypeLimit = 50)

        assertEquals(results, actual)
    }

    // Real bug this locks in: CachingGitHubSearchRepository.search() used to catch (e:
    // Exception), which also catches CancellationException (a subtype of Exception) - so a
    // search cancelled mid-flight (SearchAutocompleteUseCase's flatMapLatest does this on
    // every keystroke that supersedes an in-flight one) fell back to a stale cache entry
    // instead of the cancellation actually propagating, breaking structured concurrency.
    // Needs a fake that genuinely suspends (delay), not one that throws/returns synchronously,
    // so there's a real point to cancel the coroutine at.
    @Test
    fun cancellationWhileSearchingPropagatesInsteadOfFallingBackToCache() = runTest {
        network.results = results
        network.delayMs = 1_000
        cache.stored["kot"] = results // a fallback value IS available - proving cancellation
        // wins over it, not just "there was nothing to fall back to"

        var caught: Throwable? = null
        val job: kotlinx.coroutines.Job = launch {
            try {
                repository.search(query = "kot", perTypeLimit = 50)
            } catch (e: Throwable) {
                caught = e
                throw e
            }
        }
        runCurrent() // let the coroutine actually start and reach network.search()'s delay
        job.cancel()
        job.join()

        assertIs<CancellationException>(caught)
        assertEquals(0, cache.getCallCount)
    }

    private class FakeNetworkSource : GitHubSearchNetworkSource {
        var results: List<SearchResultItem> = emptyList()
        var error: Throwable? = null
        var delayMs: Long = 0

        override suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem> {
            if (delayMs > 0) delay(delayMs)
            error?.let { throw it }
            return results
        }
    }

    private class FakeCache : GitHubSearchCache {
        val stored: MutableMap<String, List<SearchResultItem>> =
            mutableMapOf<String, List<SearchResultItem>>()
        val putCalls: MutableList<Pair<String, List<SearchResultItem>>> =
            mutableListOf<Pair<String, List<SearchResultItem>>>()
        var getCallCount: Int = 0
            private set

        override suspend fun get(query: String): List<SearchResultItem>? {
            getCallCount++
            return stored[query]
        }

        override suspend fun put(query: String, results: List<SearchResultItem>) {
            stored[query] = results
            putCalls.add(query to results)
        }
    }
}
