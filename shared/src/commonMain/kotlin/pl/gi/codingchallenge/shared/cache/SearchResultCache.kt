package pl.gi.codingchallenge.shared.cache

import app.cash.sqldelight.db.SqlDriver
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchCache

// Network-first, cache-as-fallback-on-failure - not TTL-based. See
// CachingGitHubSearchRepository (:app) for the actual fallback decision; this class
// is just the storage - get/put, nothing about when to use which.
class SearchResultCache(driver: SqlDriver) : GitHubSearchCache {
    private val database = CacheDatabase(driver)
    private val json = Json

    override suspend fun get(query: String): List<SearchResultItem>? =
        database.cachedSearchQueries.selectByQuery(query).executeAsOneOrNull()?.let {
            json.decodeFromString(it.resultsJson)
        }

    override suspend fun put(query: String, results: List<SearchResultItem>) {
        database.cachedSearchQueries.upsert(query, json.encodeToString(results))
    }
}
