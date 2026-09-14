package pl.gi.codingchallenge.shared.domain.repository

import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

// The other half of GitHubSearchRepository's decomposition - a narrow, storage-only
// contract. SearchResultCache (:shared/cache) is the one real implementation;
// declared here (not in cache/) so CachingGitHubSearchRepository and its tests
// depend on this abstraction, not the SQLDelight-backed concrete class.
interface GitHubSearchCache {
    suspend fun get(query: String): List<SearchResultItem>?
    suspend fun put(query: String, results: List<SearchResultItem>)
}
