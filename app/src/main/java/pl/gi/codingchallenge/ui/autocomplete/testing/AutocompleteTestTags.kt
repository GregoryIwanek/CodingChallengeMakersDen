package pl.gi.codingchallenge.ui.autocomplete.testing

// Shared with androidTest (internal is visible there within the same
// module) so instrumented tests target elements by tag instead of by
// display text, which changes per AutocompleteUiState. Lives in its
// own `testing` sub-package, separate from the composable it tags, so
// it reads as test-support infrastructure rather than UI
// implementation — relevant if ui.autocomplete is ever extracted as
// a standalone library, not a project
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