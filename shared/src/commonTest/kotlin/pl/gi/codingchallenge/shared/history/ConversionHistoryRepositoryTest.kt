package pl.gi.codingchallenge.shared.history

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversionHistoryRepositoryTest {

    @Test
    fun record_appendsToHistory() = runTest {
        val repo = InMemoryConversionHistoryRepository()

        repo.record(ConversionRecord(ConversionType.CELSIUS_TO_FAHRENHEIT, input = 0.0, output = 32.0))

        assertEquals(1, repo.history.first().size)
    }

    @Test
    fun record_multipleEntries_preservesOrder() = runTest {
        val repo = InMemoryConversionHistoryRepository()
        val first = ConversionRecord(ConversionType.CELSIUS_TO_FAHRENHEIT, input = 0.0, output = 32.0)
        val second = ConversionRecord(ConversionType.KILOMETERS_TO_MILES, input = 1.0, output = 0.621371)

        repo.record(first)
        repo.record(second)

        assertEquals(listOf(first, second), repo.history.first())
    }
}
