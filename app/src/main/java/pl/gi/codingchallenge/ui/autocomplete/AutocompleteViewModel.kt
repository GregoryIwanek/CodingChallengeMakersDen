package pl.gi.codingchallenge.ui.autocomplete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import pl.gi.codingchallenge.domain.AutocompleteUiState
import pl.gi.codingchallenge.domain.SearchAutocompleteUseCase
import javax.inject.Inject

private const val STOP_TIMEOUT_MILLIS = 5_000L

@HiltViewModel
class AutocompleteViewModel @Inject constructor(
    searchAutocomplete: SearchAutocompleteUseCase,
) : ViewModel() {

    private val query = MutableStateFlow("")

    private val _uiState = observeUiState(searchAutocomplete)
    val uiState: StateFlow<AutocompleteUiState> = _uiState

    fun onQueryChanged(newQuery: String) {
        query.value = newQuery
    }

    private fun observeUiState(
        searchAutocomplete: SearchAutocompleteUseCase,
    ): StateFlow<AutocompleteUiState> =
        searchAutocomplete(query)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                AutocompleteUiState.Idle,
            )
}
