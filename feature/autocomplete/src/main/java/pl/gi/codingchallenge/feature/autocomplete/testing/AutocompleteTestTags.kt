package pl.gi.codingchallenge.feature.autocomplete.testing

// Public so tests in other modules (e.g. :app's end-to-end DetailFlowTest) can
// target elements by tag instead of by display text, which changes per
// AutocompleteUiState.
object AutocompleteTestTags {
    const val SEARCH_FIELD: String = "autocomplete_search_field"
    const val CLEAR_BUTTON: String = "autocomplete_clear_button"
    const val LEADING_ICON_BUTTON: String = "autocomplete_leading_icon_button"
    const val SUGGESTION_PANEL: String = "autocomplete_suggestion_panel"
    const val LOADING_INDICATOR: String = "autocomplete_loading_indicator"
    const val EMPTY_STATE: String = "autocomplete_empty_state"
    const val ERROR_STATE: String = "autocomplete_error_state"
    const val RETRY_BUTTON: String = "autocomplete_retry_button"
    const val RESULTS_LIST: String = "autocomplete_results_list"
    const val RESULT_COUNT_HEADER: String = "autocomplete_result_count_header"

    fun resultRow(id: String) = "autocomplete_result_row_$id"
    fun resultDivider(index: Int) = "autocomplete_result_divider_$index"
}
