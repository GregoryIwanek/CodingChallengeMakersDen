package pl.gi.codingchallenge.data.repository

import pl.gi.codingchallenge.shared.cache.SearchResultCache
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchRepository
import javax.inject.Inject

// Network-first, cache-as-fallback-on-failure - not a TTL/staleness cache. Directly
// addresses this app's documented rate-limit gap (README "Known limitations"): a
// failed search due to rate limiting returns the last-known-good result for that
// exact query instead of the generic error state, when one exists.
class CachingGitHubSearchRepository @Inject constructor(
    private val network: GitHubSearchRepositoryImpl,
    private val cache: SearchResultCache,
) : GitHubSearchRepository {

    override suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem> =
        try {
            val fresh = network.search(query, perTypeLimit)
            cache.put(query, fresh)
            fresh
        } catch (e: Exception) {
            cache.get(query) ?: throw e
        }
}
