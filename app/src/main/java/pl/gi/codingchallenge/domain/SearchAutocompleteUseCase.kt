package pl.gi.codingchallenge.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

private const val DEBOUNCE_MILLIS = 350L
private const val MIN_QUERY_LENGTH = 3

class SearchAutocompleteUseCase @Inject constructor(
    private val repository: GitHubSearchRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    operator fun invoke(queries: Flow<String>): Flow<AutocompleteUiState> =
        queries
            .debounce(DEBOUNCE_MILLIS)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.length < MIN_QUERY_LENGTH) {
                    flowOf(AutocompleteUiState.Idle)
                } else {
                    flow {
                        emit(AutocompleteUiState.Loading)
                        val results = repository.search(query)
                        emit(
                            if (results.isEmpty()) AutocompleteUiState.Empty
                            else AutocompleteUiState.Success(results),
                        )
                    }.catch { e -> emit(AutocompleteUiState.Error(e.message ?: "Unknown error")) }
                }
            }
}
