package pl.gi.codingchallenge.shared.domain.repository

import kotlinx.coroutines.CancellationException
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

// Network-first, cache-as-fallback-on-failure - not a TTL/staleness cache. Directly
// addresses this app's documented rate-limit gap (README "Known limitations"): a
// failed search due to rate limiting returns the last-known-good result for that
// exact query instead of the generic error state, when one exists.
//
// Depends on GitHubSearchNetworkSource/GitHubSearchCache (interfaces), not
// GitHubSearchNetworkSourceImpl/SearchResultCache (concrete classes) - this is what
// makes CachingGitHubSearchRepositoryTest able to fake both collaborators directly,
// no Ktor or SQLite involved in that test at all.
class CachingGitHubSearchRepository(
    private val network: GitHubSearchNetworkSource,
    private val cache: GitHubSearchCache
) : GitHubSearchRepository {

    @Throws(Exception::class)
    override suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem> = try {
        val fresh: List<SearchResultItem> = network.search(query, perTypeLimit)
        cache.put(query, fresh)
        fresh
    } catch (e: CancellationException) {
        // CancellationException is a subtype of Exception - a plain catch (e: Exception)
        // below would swallow it and fall back to a stale cache entry instead of letting
        // the cancellation propagate, breaking structured concurrency. SearchAutocompleteUseCase's
        // flatMapLatest cancels the in-flight search for every keystroke that supersedes it,
        // so this path is live in normal use, not a hypothetical.
        throw e
    } catch (e: Exception) {
        cache.get(query) ?: throw e
    }
}
