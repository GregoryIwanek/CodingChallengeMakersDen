package pl.gi.codingchallenge.ui.autocomplete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import pl.gi.codingchallenge.domain.AutocompleteUiState
import pl.gi.codingchallenge.domain.QueryRequest
import pl.gi.codingchallenge.domain.SearchAutocompleteUseCase
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class AutocompleteViewModel @Inject constructor(
    searchAutocomplete: SearchAutocompleteUseCase,
) : ViewModel() {

    private val query = MutableStateFlow(QueryRequest(text = ""))

    private val _uiState = observeUiState(searchAutocomplete)
    val uiState: StateFlow<AutocompleteUiState> = _uiState

    fun onQueryChanged(newQuery: String) {
        query.value = QueryRequest(text = newQuery)
    }

    /**
     * Re-runs the search for the current query even if it's unchanged.
     * Bumping `attempt` (not `text`) makes the emitted [QueryRequest]
     * structurally distinct, so it survives both `MutableStateFlow`'s
     * own equality suppression and the use case's `distinctUntilChanged()`
     * — a plain `onQueryChanged(sameText)` would be silently dropped by
     * both.
     */
    fun retry() {
        query.update { it.copy(attempt = it.attempt + 1) }
    }

    private fun observeUiState(
        searchAutocomplete: SearchAutocompleteUseCase,
    ): StateFlow<AutocompleteUiState> =
        searchAutocomplete(query)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = AutocompleteUiState.Idle,
            )
}
