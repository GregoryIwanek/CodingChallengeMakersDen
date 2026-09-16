package pl.gi.codingchallenge.shared.domain.repository

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
    private val cache: GitHubSearchCache,
) : GitHubSearchRepository {

    @Throws(Exception::class)
    override suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem> =
        try {
            val fresh = network.search(query, perTypeLimit)
            cache.put(query, fresh)
            fresh
        } catch (e: Exception) {
            cache.get(query) ?: throw e
        }
}
