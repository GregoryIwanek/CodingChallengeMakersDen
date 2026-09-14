package pl.gi.codingchallenge.shared.domain.repository

import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

// Narrower than GitHubSearchRepository: "raw network access to GitHub search,"
// nothing about caching or resilience. Only CachingGitHubSearchRepository (:app)
// combines this with a GitHubSearchCache to fulfill the actual GitHubSearchRepository
// contract the domain layer depends on.
interface GitHubSearchNetworkSource {
    suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem>
}
