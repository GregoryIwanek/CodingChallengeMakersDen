package pl.gi.codingchallenge.ui.autocomplete

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.gi.codingchallenge.R
import pl.gi.codingchallenge.domain.AutocompleteUiState
import pl.gi.codingchallenge.domain.model.SearchResultItem
import pl.gi.codingchallenge.util.dimRes
import pl.gi.codingchallenge.util.spRes
import pl.gi.codingchallenge.util.strRes

/**
 * Floating pill search bar that sits over whatever content the host
 * screen provides Owns only the bar and the suggestion card below it — never the backdrop,
 * scrim, or navigation, all of which are the host's responsibility.
 * Drop it onto any screen inside a Box:
 *
 * ```
 * Box(Modifier.fillMaxSize()) {
 *     HostScreenContent()
 *     GitHubAutocompleteBar(
 *         modifier = Modifier.align(Alignment.TopCenter).padding(16.dp).fillMaxWidth(),
 *         onItemClick = { navigateToDetail(it) },
 *     )
 * }
 * ```
 */
@Composable
fun GitHubAutocompleteBar(
    modifier: Modifier = Modifier,
    viewModel: AutocompleteViewModel = hiltViewModel(),
    onItemClick: (SearchResultItem) -> Unit = {},
    onLeadingIconClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GitHubAutocompleteBar(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onItemClick = onItemClick,
        onLeadingIconClick = onLeadingIconClick,
        modifier = modifier,
    )
}

/**
 * Stateless implementation — hoisted out so it can be previewed/tested
 * without Hilt. `internal` (not `private`) so androidTest, which is
 * compiled as part of this same module, can exercise it directly with
 * canned [AutocompleteUiState] values instead of going through Hilt.
 */
@Composable
internal fun GitHubAutocompleteBar(
    uiState: AutocompleteUiState,
    onQueryChanged: (String) -> Unit,
    onItemClick: (SearchResultItem) -> Unit,
    onLeadingIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialText: String = "",
    initialActive: Boolean = false,
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    var active by rememberSaveable { mutableStateOf(initialActive) }

    Column(modifier) {
        FloatingSearchBar(
            text = text,
            active = active,
            onTextChange = {
                text = it
                onQueryChanged(it)
            },
            onActiveChange = { active = it },
            onLeadingIconClick = onLeadingIconClick,
        )

        if (active) {
            Spacer(Modifier.height(dimRes(R.dimen.autocomplete_bar_to_panel_spacing)))
            SuggestionPanel {
                when (uiState) {
                    AutocompleteUiState.Idle -> {} // < 3 chars: host content stays visible behind the bar
                    AutocompleteUiState.Loading -> LoadingIndicator()
                    AutocompleteUiState.Empty -> EmptyState()
                    is AutocompleteUiState.Error -> ErrorState(uiState.message) {
                        onQueryChanged(text) // retry
                    }

                    is AutocompleteUiState.Success -> LazyColumn {
                        items(uiState.items, key = { it.id }) { item ->
                            SearchResultRow(item, onClick = { onItemClick(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingSearchBar(
    text: String,
    active: Boolean,
    onTextChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onLeadingIconClick: () -> Unit,
) {
    val barCornerRadius = RoundedCornerShape(dimRes(R.dimen.autocomplete_bar_corner_radius))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimRes(R.dimen.autocomplete_bar_height))
            .shadow(
                elevation = dimRes(R.dimen.autocomplete_bar_elevation),
                shape = barCornerRadius,
            )
            .background(color = AutocompleteColors.Surface, shape = barCornerRadius)
            .padding(horizontal = dimRes(R.dimen.autocomplete_bar_inner_padding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                if (active) onActiveChange(false)
                onLeadingIconClick()
            },
            modifier = Modifier.testTag(AutocompleteTestTags.LEADING_ICON_BUTTON),
        ) {
            Icon(
                imageVector = if (active) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
                contentDescription = if (active) {
                    strRes(R.string.autocomplete_leading_icon_back)
                } else {
                    strRes(R.string.autocomplete_leading_icon_menu)
                },
                tint = AutocompleteColors.TextSecondary,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(
                    text = strRes(R.string.autocomplete_search_placeholder),
                    style = TextStyle(
                        fontSize = spRes(R.dimen.autocomplete_input_text_size),
                        color = AutocompleteColors.TextSecondary,
                    ),
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = spRes(R.dimen.autocomplete_input_text_size),
                    color = AutocompleteColors.TextPrimary,
                ),
                cursorBrush = SolidColor(AutocompleteColors.Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AutocompleteTestTags.SEARCH_FIELD)
                    .onFocusChanged { if (it.isFocused) onActiveChange(true) },
            )
        }

        if (text.isNotEmpty()) {
            IconButton(
                onClick = { onTextChange("") },
                modifier = Modifier
                    .size(dimRes(R.dimen.autocomplete_clear_button_size))
                    .testTag(AutocompleteTestTags.CLEAR_BUTTON),
            ) {
                Box(
                    modifier = Modifier
                        .size(dimRes(R.dimen.autocomplete_clear_circle_size))
                        .background(color = AutocompleteColors.Border, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = strRes(R.string.autocomplete_clear_icon),
                        tint = AutocompleteColors.TextSecondary,
                        modifier = Modifier.size(dimRes(R.dimen.autocomplete_clear_icon_size)),
                    )
                }
            }
        } else {
            Box(Modifier.width(dimRes(R.dimen.autocomplete_leading_spacer_width)))
        }
    }
}

@Composable
private fun SuggestionPanel(content: @Composable () -> Unit) {
    val panelCornerRadius = RoundedCornerShape(dimRes(R.dimen.autocomplete_panel_corner_radius))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.SUGGESTION_PANEL)
            .shadow(
                elevation = dimRes(R.dimen.autocomplete_panel_elevation),
                shape = panelCornerRadius,
            )
            .background(color = AutocompleteColors.Surface, shape = panelCornerRadius),
    ) {
        content()
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.LOADING_INDICATOR)
            .height(dimRes(R.dimen.autocomplete_short_state_box_height)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = AutocompleteColors.Accent,
            strokeWidth = dimRes(R.dimen.autocomplete_spinner_stroke_width),
            modifier = Modifier.size(dimRes(R.dimen.autocomplete_large_state_icon_size))
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.EMPTY_STATE)
            .height(dimRes(R.dimen.autocomplete_short_state_box_height)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            strRes(R.string.autocomplete_empty_message),
            fontSize = spRes(R.dimen.autocomplete_state_headline_text_size),
            color = AutocompleteColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(
                horizontal = dimRes(R.dimen.autocomplete_state_message_horizontal_padding),
            ),
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.ERROR_STATE)
            .height(dimRes(R.dimen.autocomplete_tall_state_box_height)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(
                dimRes(R.dimen.autocomplete_error_column_spacing),
            ),
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = AutocompleteColors.ErrorColor,
                modifier = Modifier.size(dimRes(R.dimen.autocomplete_large_state_icon_size))
            )
            Text(
                strRes(R.string.autocomplete_error_headline),
                fontSize = spRes(R.dimen.autocomplete_state_headline_text_size),
                fontWeight = FontWeight.SemiBold,
                color = AutocompleteColors.TextPrimary
            )
            Text(
                message,
                fontSize = spRes(R.dimen.autocomplete_state_detail_text_size),
                color = AutocompleteColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(
                    horizontal = dimRes(R.dimen.autocomplete_state_message_horizontal_padding),
                ),
            )
            Spacer(Modifier.height(dimRes(R.dimen.autocomplete_error_icon_to_headline_spacing)))
            OutlinedButton(
                onClick = onRetry,
                contentPadding = PaddingValues(
                    horizontal = dimRes(R.dimen.autocomplete_retry_horizontal_padding),
                    vertical = dimRes(R.dimen.autocomplete_retry_vertical_padding),
                ),
                modifier = Modifier.testTag(AutocompleteTestTags.RETRY_BUTTON),
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = AutocompleteColors.Accent,
                    modifier = Modifier.size(dimRes(R.dimen.autocomplete_retry_icon_size))
                )
                Spacer(Modifier.width(dimRes(R.dimen.autocomplete_retry_icon_text_spacing)))
                Text(
                    strRes(R.string.autocomplete_retry_label),
                    fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                    fontWeight = FontWeight.SemiBold,
                    color = AutocompleteColors.Accent
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: SearchResultItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.resultRow(item.id))
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimRes(R.dimen.autocomplete_row_horizontal_padding),
                vertical = dimRes(R.dimen.autocomplete_row_vertical_padding),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (item) {
            is SearchResultItem.RepoResult -> {
                Box(
                    Modifier
                        .size(dimRes(R.dimen.autocomplete_avatar_size))
                        .background(
                            color = AutocompleteColors.RepoBg,
                            shape = RoundedCornerShape(
                                dimRes(R.dimen.autocomplete_repo_avatar_corner_radius),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.name.first().uppercase(),
                        color = AutocompleteColors.Accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = spRes(R.dimen.autocomplete_item_title_text_size)
                    )
                }
                Spacer(Modifier.width(dimRes(R.dimen.autocomplete_avatar_to_text_spacing)))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = spRes(R.dimen.autocomplete_item_title_text_size),
                        color = AutocompleteColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.description?.let { "${item.ownerLogin} · $it" } ?: item.ownerLogin,
                        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                        color = AutocompleteColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = AutocompleteColors.StarColor,
                        modifier = Modifier.size(dimRes(R.dimen.autocomplete_star_icon_size))
                    )
                    Spacer(Modifier.width(dimRes(R.dimen.autocomplete_star_icon_text_spacing)))
                    Text(
                        formatStars(item.stars),
                        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                        fontWeight = FontWeight.SemiBold,
                        color = AutocompleteColors.StarColor
                    )
                }
            }

            is SearchResultItem.UserResult -> {
                Box(
                    Modifier
                        .size(dimRes(R.dimen.autocomplete_avatar_size))
                        .background(color = AutocompleteColors.UserBg, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = AutocompleteColors.UserIcon,
                        modifier = Modifier.size(dimRes(R.dimen.autocomplete_person_icon_size))
                    )
                }
                Spacer(Modifier.width(dimRes(R.dimen.autocomplete_avatar_to_text_spacing)))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.login,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = spRes(R.dimen.autocomplete_item_title_text_size),
                        color = AutocompleteColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.htmlUrl.removePrefix("https://"),
                        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                        color = AutocompleteColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun formatStars(count: Int): String =
    if (count >= 1000) "${count / 1000}.${(count % 1000) / 100}k" else count.toString()

// ---- Previews -------------------------------------------------------
// GitHubAutocompleteBar's public overload needs Hilt (hiltViewModel()),
// which doesn't resolve in @Preview — these call the private stateless
// overload directly with canned AutocompleteUiState values instead.

private val previewResults = listOf(
    SearchResultItem.RepoResult(
        id = "1", name = "dataflow-kt", fullName = "kotlinx/dataflow-kt", ownerLogin = "kotlinx",
        avatarUrl = null, description = "Structured concurrency data pipelines", stars = 2100,
    ),
    SearchResultItem.UserResult(
        id = "2",
        login = "flowdev",
        avatarUrl = null,
        htmlUrl = "https://github.com/flowdev"
    ),
    SearchResultItem.RepoResult(
        id = "3", name = "flowmatic", fullName = "oss/flowmatic", ownerLogin = "oss",
        avatarUrl = null, description = "Reactive flow scheduler for the JVM", stars = 640,
    ),
    SearchResultItem.UserResult(
        id = "4",
        login = "flowraven",
        avatarUrl = null,
        htmlUrl = "https://github.com/flowraven"
    ),
)

@Preview(showBackground = true, widthDp = 380, heightDp = 500)
@Composable
private fun GitHubAutocompleteBarSuccessPreview() {
    GitHubAutocompleteBar(
        uiState = AutocompleteUiState.Success(previewResults),
        onQueryChanged = {},
        onItemClick = {},
        onLeadingIconClick = {},
        initialText = "flow",
        initialActive = true,
    )
}

@Preview(name = "Loading", showBackground = true, widthDp = 380, heightDp = 300)
@Composable
private fun GitHubAutocompleteBarLoadingPreview() {
    GitHubAutocompleteBar(
        uiState = AutocompleteUiState.Loading,
        onQueryChanged = {},
        onItemClick = {},
        onLeadingIconClick = {},
        initialText = "flow",
        initialActive = true,
    )
}

@Preview(name = "Empty", showBackground = true, widthDp = 380, heightDp = 300)
@Composable
private fun GitHubAutocompleteBarEmptyPreview() {
    GitHubAutocompleteBar(
        uiState = AutocompleteUiState.Empty,
        onQueryChanged = {},
        onItemClick = {},
        onLeadingIconClick = {},
        initialText = "zzz",
        initialActive = true,
    )
}

@Preview(name = "Error", showBackground = true, widthDp = 380, heightDp = 320)
@Composable
private fun GitHubAutocompleteBarErrorPreview() {
    GitHubAutocompleteBar(
        uiState = AutocompleteUiState.Error("Check your connection and try again."),
        onQueryChanged = {},
        onItemClick = {},
        onLeadingIconClick = {},
        initialText = "flow",
        initialActive = true,
    )
}

@Preview(name = "Idle", showBackground = true, widthDp = 380, heightDp = 120)
@Composable
private fun GitHubAutocompleteBarIdlePreview() {
    GitHubAutocompleteBar(
        uiState = AutocompleteUiState.Idle,
        onQueryChanged = {},
        onItemClick = {},
        onLeadingIconClick = {},
    )
}
