package pl.gi.codingchallenge.shared.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

interface ConversionHistoryRepository {
    val history: Flow<List<ConversionRecord>>
    suspend fun record(entry: ConversionRecord)
}

class InMemoryConversionHistoryRepository : ConversionHistoryRepository {
    private val _history = MutableStateFlow<List<ConversionRecord>>(emptyList())
    override val history: Flow<List<ConversionRecord>> = _history.asStateFlow()
    override suspend fun record(entry: ConversionRecord) {
        _history.update { it + entry }
    }
}
