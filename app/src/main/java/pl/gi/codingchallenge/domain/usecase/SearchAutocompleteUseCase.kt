package pl.gi.codingchallenge.domain.usecase

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
import pl.gi.codingchallenge.domain.model.QueryRequest
import pl.gi.codingchallenge.domain.model.SearchOutcome
import pl.gi.codingchallenge.domain.repository.GitHubSearchRepository

private const val DEBOUNCE_MILLIS = 350L
private const val MIN_QUERY_LENGTH = 3

class SearchAutocompleteUseCase @Inject constructor(
    private val repository: GitHubSearchRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    operator fun invoke(requests: Flow<QueryRequest>): Flow<SearchOutcome> =
        requests
            .debounce(DEBOUNCE_MILLIS)
            .distinctUntilChanged()
            .flatMapLatest { request ->
                val query = request.text
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
