package pl.gi.codingchallenge.shared.cache

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

class SearchResultCacheTest {

    private fun newCache(): SearchResultCache {
        val driver: app.cash.sqldelight.db.SqlDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // Unlike AndroidSqliteDriver, this driver does NOT create the schema
        // automatically.
        CacheDatabase.Schema.create(driver)
        return SearchResultCache(driver)
    }

    @Test
    fun get_missingQuery_returnsNull() = runTest {
        assertNull(newCache().get("nothing-cached-yet"))
    }

    @Test
    fun put_thenGet_roundTrips() = runTest {
        val cache: SearchResultCache = newCache()
        val results: List<SearchResultItem> = listOf(
            SearchResultItem.UserResult(
                id = "1",
                login = "octocat",
                avatarUrl = null,
                htmlUrl = "https://github.com/octocat"
            )
        )

        cache.put("octocat", results)

        assertEquals(results, cache.get("octocat"))
    }

    @Test
    fun put_beyondCap_evictsOldestQueriesFirst() = runTest {
        val cache: SearchResultCache = newCache()
        val results: List<SearchResultItem> = emptyList<SearchResultItem>()
        val cap: Int = SearchResultCache.MAX_CACHED_QUERIES.toInt()

        repeat(cap + 5) { cache.put("query-$it", results) }

        // The first 5 written (oldest by write order) should be evicted...
        repeat(5) { assertNull(cache.get("query-$it")) }
        // ...while the most recent MAX_CACHED_QUERIES survive.
        for (i in 5 until cap + 5) {
            assertEquals(results, cache.get("query-$i"))
        }
    }

    @Test
    fun put_reUpsertingAnExistingQuery_refreshesItsRecency() = runTest {
        val cache: SearchResultCache = newCache()
        val results: List<SearchResultItem> = emptyList<SearchResultItem>()
        val cap: Int = SearchResultCache.MAX_CACHED_QUERIES.toInt()

        cache.put("stays-alive", results)
        // Re-upsert the same key partway through, then fill the rest of the
        // cap with brand-new queries - "stays-alive" should survive even
        // though it was the very first key ever written.
        repeat(cap) { cache.put("filler-$it", results) }
        cache.put("stays-alive", results)
        repeat(4) { cache.put("more-filler-$it", results) }

        assertEquals(results, cache.get("stays-alive"))
    }
}
