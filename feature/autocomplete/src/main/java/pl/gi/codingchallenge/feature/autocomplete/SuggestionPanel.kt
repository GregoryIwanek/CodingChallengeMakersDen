package pl.gi.codingchallenge.feature.autocomplete

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import pl.gi.codingchallenge.feature.autocomplete.testing.AutocompleteTestTags
import pl.gi.codingchallenge.feature.autocomplete.util.spRes
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

// Mirrors :shared's internal MAX_RESULTS; hosts override it via the
// maxResults param if the shared cap ever changes.
internal const val DEFAULT_MAX_RESULTS: Int = 50

internal fun isResultCountCapped(count: Int, maxResults: Int): Boolean = count >= maxResults

@Composable
internal fun SuggestionPanel(
    uiState: AutocompleteUiState,
    text: String,
    onRetry: () -> Unit,
    onItemClick: (SearchResultItem) -> Unit,
    maxResults: Int = DEFAULT_MAX_RESULTS
) {
    val panelCornerRadius: RoundedCornerShape =
        RoundedCornerShape(dimensionResource(R.dimen.autocomplete_panel_corner_radius))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.SUGGESTION_PANEL)
            .shadow(
                elevation = dimensionResource(R.dimen.autocomplete_panel_elevation),
                shape = panelCornerRadius
            )
            .background(
                color = colorResource(R.color.autocomplete_surface),
                shape = panelCornerRadius
            )
    ) {
        when (uiState) {
            // Never shown — see AnimatedVisibility's visible condition.
            AutocompleteUiState.Idle -> {}
            AutocompleteUiState.Loading -> LoadingIndicator()
            AutocompleteUiState.Empty -> EmptyState()
            is AutocompleteUiState.Error -> ErrorState(uiState.message, onRetry = onRetry)

            // The height cap wraps header + list so the whole panel, not
            // just the list, stays within autocomplete_panel_max_height.
            is AutocompleteUiState.Success -> Column(
                modifier = Modifier.heightIn(
                    max = dimensionResource(R.dimen.autocomplete_panel_max_height)
                )
            ) {
                ResultCountHeader(
                    count = uiState.items.size,
                    isCapped = isResultCountCapped(
                        count = uiState.items.size,
                        maxResults = maxResults
                    )
                )
                HorizontalDivider(color = colorResource(R.color.autocomplete_divider))
                LazyColumn(
                    // Keyed on text (not the whole uiState) so a retry of the
                    // same query keeps its scroll position, but a genuinely
                    // new query starts scrolled to the top.
                    state = remember(text) { LazyListState() },
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .testTag(AutocompleteTestTags.RESULTS_LIST)
                ) {
                    itemsIndexed(uiState.items, key = { _, item ->
                        item.uniqueKey
                    }) { index, item ->
                        SearchResultRow(item, onClick = { onItemClick(item) })
                        if (index < uiState.items.lastIndex) {
                            HorizontalDivider(
                                color = colorResource(R.color.autocomplete_divider),
                                modifier = Modifier.testTag(
                                    AutocompleteTestTags.resultDivider(index)
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// Takes Resources (not Context) so JVM tests can pass Paparazzi's.
internal fun formatResultCount(resources: Resources, count: Int, isCapped: Boolean): String =
    if (isCapped) {
        resources.getString(R.string.autocomplete_result_count_capped, count)
    } else {
        resources.getQuantityString(R.plurals.autocomplete_result_count, count, count)
    }

@Composable
private fun ResultCountHeader(count: Int, isCapped: Boolean) {
    Text(
        // LocalResources (not LocalContext) so a config change recomposes this.
        formatResultCount(resources = LocalResources.current, count = count, isCapped = isCapped),
        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
        color = colorResource(R.color.autocomplete_text_secondary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                // Matches SearchResultRow so the header text lines up with row content.
                horizontal = dimensionResource(R.dimen.autocomplete_row_horizontal_padding),
                vertical = dimensionResource(R.dimen.autocomplete_result_count_vertical_padding)
            )
            .semantics {
                heading()
                liveRegion = LiveRegionMode.Polite
            }
            .testTag(AutocompleteTestTags.RESULT_COUNT_HEADER)
    )
}

@Composable
private fun LoadingIndicator() {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.LOADING_INDICATOR)
            .height(dimensionResource(R.dimen.autocomplete_short_state_box_height)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = colorResource(R.color.autocomplete_accent),
            strokeWidth = dimensionResource(R.dimen.autocomplete_spinner_stroke_width),
            modifier = Modifier.size(dimensionResource(R.dimen.autocomplete_large_state_icon_size))
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.EMPTY_STATE)
            .height(dimensionResource(R.dimen.autocomplete_short_state_box_height)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            stringResource(R.string.autocomplete_empty_message),
            fontSize = spRes(R.dimen.autocomplete_state_headline_text_size),
            color = colorResource(R.color.autocomplete_text_secondary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                horizontal = dimensionResource(
                    R.dimen.autocomplete_state_message_horizontal_padding
                )
            )
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.ERROR_STATE)
            .height(dimensionResource(R.dimen.autocomplete_tall_state_box_height)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.autocomplete_error_column_spacing)
            )
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = colorResource(R.color.autocomplete_error_color),
                modifier = Modifier.size(
                    dimensionResource(R.dimen.autocomplete_large_state_icon_size)
                )
            )
            Text(
                stringResource(R.string.autocomplete_error_headline),
                fontSize = spRes(R.dimen.autocomplete_state_headline_text_size),
                fontWeight = FontWeight.SemiBold,
                color = colorResource(R.color.autocomplete_text_primary)
            )
            Text(
                message,
                fontSize = spRes(R.dimen.autocomplete_state_detail_text_size),
                color = colorResource(R.color.autocomplete_text_secondary),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    horizontal = dimensionResource(
                        R.dimen.autocomplete_state_message_horizontal_padding
                    )
                )
            )
            Spacer(
                Modifier.height(
                    dimensionResource(R.dimen.autocomplete_error_icon_to_headline_spacing)
                )
            )
            OutlinedButton(
                onClick = onRetry,
                contentPadding = PaddingValues(
                    horizontal = dimensionResource(R.dimen.autocomplete_retry_horizontal_padding),
                    vertical = dimensionResource(R.dimen.autocomplete_retry_vertical_padding)
                ),
                modifier = Modifier.testTag(AutocompleteTestTags.RETRY_BUTTON)
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = colorResource(R.color.autocomplete_accent),
                    modifier = Modifier.size(
                        dimensionResource(R.dimen.autocomplete_retry_icon_size)
                    )
                )
                Spacer(
                    Modifier.width(dimensionResource(R.dimen.autocomplete_retry_icon_text_spacing))
                )
                Text(
                    stringResource(R.string.autocomplete_retry_label),
                    fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                    fontWeight = FontWeight.SemiBold,
                    color = colorResource(R.color.autocomplete_accent)
                )
            }
        }
    }
}

@Preview(name = "Loading", showBackground = true, widthDp = 380, heightDp = 160)
@Composable
private fun SuggestionPanelLoadingPreview() {
    SuggestionPanel(uiState = AutocompleteUiState.Loading, text = "kot", onRetry = {
    }, onItemClick = {})
}

@Preview(name = "Empty", showBackground = true, widthDp = 380, heightDp = 160)
@Composable
private fun SuggestionPanelEmptyPreview() {
    SuggestionPanel(uiState = AutocompleteUiState.Empty, text = "zzz", onRetry = {
    }, onItemClick = {})
}

@Preview(name = "Error", showBackground = true, widthDp = 380, heightDp = 190)
@Composable
private fun SuggestionPanelErrorPreview() {
    SuggestionPanel(
        uiState = AutocompleteUiState.Error("Check your connection and try again."),
        text = "kot",
        onRetry = {},
        onItemClick = {}
    )
}

@Preview(name = "Success", showBackground = true, widthDp = 380, heightDp = 200)
@Composable
private fun SuggestionPanelSuccessPreview() {
    SuggestionPanel(
        uiState = AutocompleteUiState.Success(
            listOf(
                SearchResultItem.RepoResult(
                    id = "1",
                    name = "kotlin",
                    fullName = "JetBrains/kotlin",
                    ownerLogin = "JetBrains",
                    avatarUrl = null,
                    description = "The Kotlin Programming Language",
                    stars = 48000
                ),
                SearchResultItem.UserResult(
                    id = "2",
                    login = "octocat",
                    avatarUrl = null,
                    htmlUrl = "https://github.com/octocat"
                )
            )
        ),
        text = "kot",
        onRetry = {},
        onItemClick = {}
    )
}
