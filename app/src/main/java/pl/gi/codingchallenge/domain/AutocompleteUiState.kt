package pl.gi.codingchallenge.domain

import pl.gi.codingchallenge.domain.model.SearchResultItem

sealed interface AutocompleteUiState {
    data object Idle : AutocompleteUiState // < 3 chars
    data object Loading : AutocompleteUiState
    data class Success(val items: List<SearchResultItem>) : AutocompleteUiState
    data object Empty : AutocompleteUiState
    data class Error(val message: String) : AutocompleteUiState
}
