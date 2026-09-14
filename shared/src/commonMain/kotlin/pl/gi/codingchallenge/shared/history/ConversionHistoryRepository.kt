package pl.gi.codingchallenge.shared.history

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// The "data layer" of this toy feature, coded to an interface even though only one
// implementation exists. This previews the interface+DI pattern used for real
// domain/data code once it migrates into :shared - the interface stays put in
// commonMain, but what implements it can change (e.g. a persisted repository)
// without touching any caller.
interface ConversionHistoryRepository {
    val history: Flow<List<ConversionRecord>>
    suspend fun record(entry: ConversionRecord)
}

// In-memory only - no persistence (that's a later, optional step). Exposed as a
// Flow so :app's ViewModel can collect it reactively, the same shape a real,
// persisted data source would use.
class InMemoryConversionHistoryRepository : ConversionHistoryRepository {
    private val _history = MutableStateFlow<List<ConversionRecord>>(emptyList())
    override val history: Flow<List<ConversionRecord>> = _history.asStateFlow()
    override suspend fun record(entry: ConversionRecord) {
        _history.update { it + entry }
    }
}
