package pl.gi.codingchallenge.ui.autocomplete

// Shared with androidTest (internal is visible there within the same
// module) so instrumented tests target elements by tag instead of by
// display text, which changes per AutocompleteUiState.
internal object AutocompleteTestTags {
    const val SEARCH_FIELD = "autocomplete_search_field"
    const val CLEAR_BUTTON = "autocomplete_clear_button"
    const val LEADING_ICON_BUTTON = "autocomplete_leading_icon_button"
    const val SUGGESTION_PANEL = "autocomplete_suggestion_panel"
    const val LOADING_INDICATOR = "autocomplete_loading_indicator"
    const val EMPTY_STATE = "autocomplete_empty_state"
    const val ERROR_STATE = "autocomplete_error_state"
    const val RETRY_BUTTON = "autocomplete_retry_button"

    fun resultRow(id: String) = "autocomplete_result_row_$id"
}