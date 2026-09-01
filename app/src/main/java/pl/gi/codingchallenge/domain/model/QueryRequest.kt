package pl.gi.codingchallenge.domain.model

/**
 * A search query plus a retry counter. `attempt` exists so retrying the *same*
 * [text] still produces a structurally distinct value — otherwise `MutableStateFlow`
 * and [SearchAutocompleteUseCase]'s `distinctUntilChanged()` would both no-op it.
 */
data class QueryRequest(val text: String, val attempt: Int = 0)
