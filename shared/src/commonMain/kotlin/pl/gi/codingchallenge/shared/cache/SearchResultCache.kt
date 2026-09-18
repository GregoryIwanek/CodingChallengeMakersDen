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
        database.cachedSearchQueries.evictOldestBeyondCap(MAX_CACHED_QUERIES)
    }

    internal companion object {
        // AT-3: without this, cachedSearch grows one row per distinct query
        // string forever (SearchAutocompleteUseCase debounces per keystroke
        // past 3 characters, so one typed word can write several rows).
        // 50 mirrors SearchLimits.MAX_RESULTS as "a reasonable-sounding cap
        // for this app's realistic usage," not a shared constant - the two
        // caps bound unrelated things (cached query strings vs result items).
        const val MAX_CACHED_QUERIES = 50L
    }
}
