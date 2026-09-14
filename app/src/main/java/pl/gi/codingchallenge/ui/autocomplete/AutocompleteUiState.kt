package pl.gi.codingchallenge.ui.autocomplete

import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

sealed interface AutocompleteUiState {
    data object Idle : AutocompleteUiState // < 3 chars
    data object Loading : AutocompleteUiState
    data class Success(val items: List<SearchResultItem>) : AutocompleteUiState
    data object Empty : AutocompleteUiState
    data class Error(val message: String) : AutocompleteUiState
}
