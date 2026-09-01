package pl.gi.codingchallenge.domain.repository

import pl.gi.codingchallenge.domain.model.MAX_RESULTS
import pl.gi.codingchallenge.domain.model.SearchResultItem

interface GitHubSearchRepository {
    suspend fun search(query: String, perTypeLimit: Int = MAX_RESULTS): List<SearchResultItem>
}
