package pl.gi.codingchallenge.feature.autocomplete

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers only the cap boundary; the plural text itself needs Resources
 * and is covered by the Paparazzi snapshots.
 */
class ResultCountTest {

    @Test
    fun `one below the cap is not capped`() {
        assertFalse(isResultCountCapped(count = 49, maxResults = 50))
    }

    @Test
    fun `exactly the cap is capped`() {
        assertTrue(isResultCountCapped(count = 50, maxResults = 50))
    }

    @Test
    fun `above the cap is capped`() {
        assertTrue(isResultCountCapped(count = 51, maxResults = 50))
    }
}
