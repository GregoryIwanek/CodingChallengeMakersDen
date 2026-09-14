package pl.gi.codingchallenge.shared.domain.repository

import pl.gi.codingchallenge.shared.domain.model.MAX_RESULTS
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

interface GitHubSearchRepository {
    suspend fun search(query: String, perTypeLimit: Int = MAX_RESULTS): List<SearchResultItem>
}
