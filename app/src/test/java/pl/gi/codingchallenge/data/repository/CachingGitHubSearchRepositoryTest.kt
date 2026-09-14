package pl.gi.codingchallenge.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchCache
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchNetworkSource

// Depends on GitHubSearchNetworkSource/GitHubSearchCache directly - no Retrofit, no
// SQLite/SQLDelight driver anywhere in this test. This is the concrete payoff of
// depending on the narrower interfaces instead of the concrete
// GitHubSearchNetworkSourceImpl/SearchResultCache classes.
class CachingGitHubSearchRepositoryTest {

    private val network = mockk<GitHubSearchNetworkSource>()
    private val cache = mockk<GitHubSearchCache>(relaxUnitFun = true)
    private val repository = CachingGitHubSearchRepository(network = network, cache = cache)

    private val results = listOf(
        SearchResultItem.RepoResult(
            id = "1", name = "kotlin", fullName = "JetBrains/kotlin",
            ownerLogin = "JetBrains", avatarUrl = null, description = null, stars = 50_000,
        ),
    )

    @Test
    fun `search success - fetches from network and caches the result`() = runTest {
        coEvery { network.search("kot", 50) } returns results

        val actual = repository.search(query = "kot", perTypeLimit = 50)

        assertEquals(results, actual)
        coVerify(exactly = 1) { cache.put("kot", results) }
    }

    @Test
    fun `search failure with a cached entry - falls back to the cache, no crash`() = runTest {
        coEvery { network.search("kot", 50) } throws RuntimeException("offline")
        coEvery { cache.get("kot") } returns results

        val actual = repository.search(query = "kot", perTypeLimit = 50)

        assertEquals(results, actual)
    }

    @Test(expected = RuntimeException::class)
    fun `search failure with no cached entry - rethrows the original exception`() = runTest {
        coEvery { network.search("kot", 50) } throws RuntimeException("offline")
        coEvery { cache.get("kot") } returns null

        repository.search(query = "kot", perTypeLimit = 50)
    }
}
