package pl.gi.codingchallenge.ui.autocomplete.testing

// internal (not private) so androidTest can target elements by tag
// instead of by display text, which changes per AutocompleteUiState.
internal object AutocompleteTestTags {
    const val SEARCH_FIELD = "autocomplete_search_field"
    const val CLEAR_BUTTON = "autocomplete_clear_button"
    const val LEADING_ICON_BUTTON = "autocomplete_leading_icon_button"
    const val SUGGESTION_PANEL = "autocomplete_suggestion_panel"
    const val LOADING_INDICATOR = "autocomplete_loading_indicator"
    const val EMPTY_STATE = "autocomplete_empty_state"
    const val ERROR_STATE = "autocomplete_error_state"
    const val RETRY_BUTTON = "autocomplete_retry_button"
    const val RESULTS_LIST = "autocomplete_results_list"

    fun resultRow(id: String) = "autocomplete_result_row_$id"
    fun resultDivider(index: Int) = "autocomplete_result_divider_$index"
}
