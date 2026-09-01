package pl.gi.codingchallenge.domain

import pl.gi.codingchallenge.domain.model.SearchResultItem

interface GitHubSearchRepository {
    suspend fun search(query: String, perTypeLimit: Int = MAX_RESULTS): List<SearchResultItem>
}
