package pl.gi.codingchallenge.shared.cache

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchResultCacheTest {

    private fun newCache(): SearchResultCache {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
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
        val cache = newCache()
        val results = listOf(
            SearchResultItem.UserResult(
                id = "1",
                login = "octocat",
                avatarUrl = null,
                htmlUrl = "https://github.com/octocat",
            ),
        )

        cache.put("octocat", results)

        assertEquals(results, cache.get("octocat"))
    }
}
