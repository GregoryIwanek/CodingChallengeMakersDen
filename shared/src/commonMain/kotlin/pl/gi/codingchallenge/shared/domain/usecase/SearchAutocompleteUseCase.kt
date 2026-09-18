package pl.gi.codingchallenge.shared.domain.usecase

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import pl.gi.codingchallenge.shared.domain.model.QueryRequest
import pl.gi.codingchallenge.shared.domain.model.SearchOutcome
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchRepository

private const val DEBOUNCE_MILLIS = 350L
private const val MIN_QUERY_LENGTH = 3

class SearchAutocompleteUseCase(private val repository: GitHubSearchRepository) {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    operator fun invoke(requests: Flow<QueryRequest>): Flow<SearchOutcome> = requests
        .debounce(DEBOUNCE_MILLIS)
        .distinctUntilChanged()
        .flatMapLatest { request ->
            // Trimmed before the length check and before it reaches the repository/cache
            // key - a whitespace-only or padded query otherwise passes the length gate
            // as-is and fragments the cache ("kotlin" vs "kotlin " being different cache
            // rows). iOS's ContentView.swift bypasses this use case entirely (Flow doesn't
            // export cleanly to Swift) and needs its own copy of this same fix.
            val query = request.text.trim()
            if (query.length < MIN_QUERY_LENGTH) {
                flowOf(SearchOutcome.QueryTooShort)
            } else {
                flow {
                    emit(SearchOutcome.Loading)
                    emit(SearchOutcome.Success(repository.search(query)))
                }.catch { e -> emit(SearchOutcome.Failure(e.message ?: "Unknown error")) }
            }
        }
}
