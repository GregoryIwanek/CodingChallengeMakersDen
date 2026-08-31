package pl.gi.codingchallenge.domain

/**
 * A search query plus a retry counter. `attempt` exists so a retry of
 * the *same* [text] still produces a distinct value — `MutableStateFlow`
 * suppresses consecutive equal values, and [SearchAutocompleteUseCase]
 * applies `distinctUntilChanged()` on top of that, so retrying with
 * just the text alone is a guaranteed no-op. Bumping `attempt` (leaving
 * `text` unchanged) makes the whole value structurally different,
 * letting the retry through both layers.
 */
data class QueryRequest(val text: String, val attempt: Int = 0)
