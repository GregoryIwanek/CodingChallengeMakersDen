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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.gi.codingchallenge.R
import pl.gi.codingchallenge.domain.AutocompleteUiState
import pl.gi.codingchallenge.domain.model.SearchResultItem
import pl.gi.codingchallenge.ui.autocomplete.testing.AutocompleteTestTags
import pl.gi.codingchallenge.util.colRes
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
fun GitHubAutocompleteBarComponent(
    modifier: Modifier = Modifier,
    viewModel: AutocompleteViewModel = hiltViewModel(),
    onItemClick: (SearchResultItem) -> Unit = {},
    onLeadingIconClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GitHubAutocompleteBarComponent(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onRetry = viewModel::retry,
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
internal fun GitHubAutocompleteBarComponent(
    uiState: AutocompleteUiState,
    onQueryChanged: (String) -> Unit,
    onRetry: () -> Unit,
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
                    is AutocompleteUiState.Error -> ErrorState(uiState.message, onRetry = onRetry)

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
    val focusManager = LocalFocusManager.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(dimRes(R.dimen.autocomplete_bar_height))
            .shadow(
                elevation = dimRes(R.dimen.autocomplete_bar_elevation),
                shape = barCornerRadius,
            )
            .background(color = colRes(R.color.autocomplete_surface), shape = barCornerRadius)
            .padding(horizontal = dimRes(R.dimen.autocomplete_bar_inner_padding)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (text.isNotEmpty()) {
            IconButton(
                onClick = {
                    val nowActive = !active
                    onActiveChange(nowActive)
                    if (!nowActive) {
                        // Clear focus so a later tap on the field fires a fresh
                        // focus-gained event and reopens the panel (see
                        // onFocusChanged below) — otherwise the field stays
                        // focused from before and a re-tap is a no-op.
                        focusManager.clearFocus()
                    }
                    onLeadingIconClick()
                },
                modifier = Modifier.testTag(AutocompleteTestTags.LEADING_ICON_BUTTON),
            ) {
                Icon(
                    imageVector = if (active) {
                        Icons.Filled.KeyboardArrowUp
                    } else {
                        Icons.Filled.KeyboardArrowDown
                    },
                    contentDescription = if (active) {
                        strRes(R.string.autocomplete_leading_icon_collapse)
                    } else {
                        strRes(R.string.autocomplete_leading_icon_expand)
                    },
                    tint = colRes(R.color.autocomplete_text_secondary),
                )
            }
        }

        val textStartPadding = if (text.isEmpty()) {
            dimRes(R.dimen.autocomplete_text_start_padding_no_leading_icon)
        } else {
            0.dp
        }

        Box(modifier = Modifier.weight(1f).padding(start = textStartPadding)) {
            if (text.isEmpty()) {
                Text(
                    text = strRes(R.string.autocomplete_search_placeholder),
                    style = TextStyle(
                        fontSize = spRes(R.dimen.autocomplete_input_text_size),
                        color = colRes(R.color.autocomplete_text_secondary),
                    ),
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(
                    fontSize = spRes(R.dimen.autocomplete_input_text_size),
                    color = colRes(R.color.autocomplete_text_primary),
                ),
                cursorBrush = SolidColor(colRes(R.color.autocomplete_accent)),
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
                        .background(color = colRes(R.color.autocomplete_border), shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = strRes(R.string.autocomplete_clear_icon),
                        tint = colRes(R.color.autocomplete_text_secondary),
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
            .background(color = colRes(R.color.autocomplete_surface), shape = panelCornerRadius),
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
            color = colRes(R.color.autocomplete_accent),
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
            color = colRes(R.color.autocomplete_text_secondary),
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
                tint = colRes(R.color.autocomplete_error_color),
                modifier = Modifier.size(dimRes(R.dimen.autocomplete_large_state_icon_size))
            )
            Text(
                strRes(R.string.autocomplete_error_headline),
                fontSize = spRes(R.dimen.autocomplete_state_headline_text_size),
                fontWeight = FontWeight.SemiBold,
                color = colRes(R.color.autocomplete_text_primary)
            )
            Text(
                message,
                fontSize = spRes(R.dimen.autocomplete_state_detail_text_size),
                color = colRes(R.color.autocomplete_text_secondary),
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
                    tint = colRes(R.color.autocomplete_accent),
                    modifier = Modifier.size(dimRes(R.dimen.autocomplete_retry_icon_size))
                )
                Spacer(Modifier.width(dimRes(R.dimen.autocomplete_retry_icon_text_spacing)))
                Text(
                    strRes(R.string.autocomplete_retry_label),
                    fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                    fontWeight = FontWeight.SemiBold,
                    color = colRes(R.color.autocomplete_accent)
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
                            color = colRes(R.color.autocomplete_repo_bg),
                            shape = RoundedCornerShape(
                                dimRes(R.dimen.autocomplete_repo_avatar_corner_radius),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.name.first().uppercase(),
                        color = colRes(R.color.autocomplete_accent),
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
                        color = colRes(R.color.autocomplete_text_primary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.description?.let { "${item.ownerLogin} · $it" } ?: item.ownerLogin,
                        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                        color = colRes(R.color.autocomplete_text_secondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = colRes(R.color.autocomplete_star_color),
                        modifier = Modifier.size(dimRes(R.dimen.autocomplete_star_icon_size))
                    )
                    Spacer(Modifier.width(dimRes(R.dimen.autocomplete_star_icon_text_spacing)))
                    Text(
                        formatStars(item.stars),
                        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                        fontWeight = FontWeight.SemiBold,
                        color = colRes(R.color.autocomplete_star_color)
                    )
                }
            }

            is SearchResultItem.UserResult -> {
                Box(
                    Modifier
                        .size(dimRes(R.dimen.autocomplete_avatar_size))
                        .background(color = colRes(R.color.autocomplete_user_bg), shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = colRes(R.color.autocomplete_user_icon),
                        modifier = Modifier.size(dimRes(R.dimen.autocomplete_person_icon_size))
                    )
                }
                Spacer(Modifier.width(dimRes(R.dimen.autocomplete_avatar_to_text_spacing)))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.login,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = spRes(R.dimen.autocomplete_item_title_text_size),
                        color = colRes(R.color.autocomplete_text_primary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.htmlUrl.removePrefix("https://"),
                        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                        color = colRes(R.color.autocomplete_text_secondary),
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
