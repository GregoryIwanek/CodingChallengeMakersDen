package pl.gi.codingchallenge.domain

import pl.gi.codingchallenge.domain.model.SearchResultItem

/**
 * Domain-only outcome of a search — no UI/presentation concepts. Notably
 * no "Empty" variant: an empty result set is just [Success] with an empty
 * list; deciding to render that as an "empty state" is a UI concern, left
 * to whoever maps this into a presentation-layer state.
 */
sealed interface SearchOutcome {
    /** The query is below the minimum length this business rule requires. */
    data object QueryTooShort : SearchOutcome
    data object Loading : SearchOutcome
    data class Success(val items: List<SearchResultItem>) : SearchOutcome
    data class Failure(val message: String) : SearchOutcome
}
