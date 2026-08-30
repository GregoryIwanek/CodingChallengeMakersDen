package pl.gi.codingchallenge.domain

import pl.gi.codingchallenge.domain.model.SearchResultItem

interface GitHubSearchRepository {
    suspend fun search(query: String, perTypeLimit: Int = 50): List<SearchResultItem>
}
