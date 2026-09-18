package pl.gi.codingchallenge.ui.autocomplete

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.gi.codingchallenge.R
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.util.dimRes

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
    onLeadingIconClick: () -> Unit = {}
) {
    val uiState: AutocompleteUiState by viewModel.uiState.collectAsStateWithLifecycle()

    GitHubAutocompleteBarComponent(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onRetry = viewModel::retry,
        onItemClick = onItemClick,
        onLeadingIconClick = onLeadingIconClick,
        modifier = modifier
    )
}

/**
 * Stateless implementation, hoisted out for preview/test access without Hilt.
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
    initialActive: Boolean = false
) {
    var text: String by rememberSaveable { mutableStateOf(initialText) }
    var active: Boolean by rememberSaveable { mutableStateOf(initialActive) }
    val hasSuggestions: Boolean = uiState != AutocompleteUiState.Idle
    val focusManager: FocusManager = LocalFocusManager.current

    // Clearing focus (see FloatingSearchBar's leading icon) is what lets a
    // later tap on the field re-fire onFocusChanged and reopen the panel.
    BackHandler(enabled = active) {
        active = false
        focusManager.clearFocus()
    }

    Column(modifier) {
        FloatingSearchBar(
            text = text,
            active = active,
            hasSuggestions = hasSuggestions,
            onTextChange = {
                text = it
                onQueryChanged(it)
            },
            onActiveChange = { active = it },
            onLeadingIconClick = onLeadingIconClick
        )

        // Idle (< 3 chars) renders no panel at all — an empty SuggestionPanel
        // would still draw its shadow/background as an ownerless floating card.
        AnimatedVisibility(
            visible = active && hasSuggestions,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
        ) {
            Column {
                Spacer(Modifier.height(dimRes(R.dimen.autocomplete_bar_to_panel_spacing)))
                SuggestionPanel(
                    uiState = uiState,
                    text = text,
                    onRetry = onRetry,
                    onItemClick = onItemClick
                )
            }
        }
    }
}

// GitHubAutocompleteBar's public overload needs Hilt (hiltViewModel()),
// which doesn't resolve in @Preview — these call the internal stateless
// overload directly with canned AutocompleteUiState values instead.

private val previewResults: List<SearchResultItem> = listOf(
    SearchResultItem.RepoResult(
        id = "1",
        name = "dataflow-kt",
        fullName = "kotlinx/dataflow-kt",
        ownerLogin = "kotlinx",
        avatarUrl = null,
        description = "Structured concurrency data pipelines",
        stars = 2100
    ),
    SearchResultItem.UserResult(
        id = "2",
        login = "flowdev",
        avatarUrl = null,
        htmlUrl = "https://github.com/flowdev"
    ),
    SearchResultItem.RepoResult(
        id = "3",
        name = "flowmatic",
        fullName = "oss/flowmatic",
        ownerLogin = "oss",
        avatarUrl = null,
        description = "Reactive flow scheduler for the JVM",
        stars = 640
    ),
    SearchResultItem.UserResult(
        id = "4",
        login = "flowraven",
        avatarUrl = null,
        htmlUrl = "https://github.com/flowraven"
    )
)

@Preview(showBackground = true, widthDp = 380, heightDp = 500)
@Composable
private fun GitHubAutocompleteBarSuccessPreview() {
    GitHubAutocompleteBarComponent(
        uiState = AutocompleteUiState.Success(previewResults),
        onQueryChanged = {},
        onRetry = {},
        onItemClick = {},
        onLeadingIconClick = {},
        initialText = "flow",
        initialActive = true
    )
}

@Preview(name = "Loading", showBackground = true, widthDp = 380, heightDp = 300)
@Composable
private fun GitHubAutocompleteBarLoadingPreview() {
    GitHubAutocompleteBarComponent(
        uiState = AutocompleteUiState.Loading,
        onQueryChanged = {},
        onRetry = {},
        onItemClick = {},
        onLeadingIconClick = {},
        initialText = "flow",
        initialActive = true
    )
}

@Preview(name = "Empty", showBackground = true, widthDp = 380, heightDp = 300)
@Composable
private fun GitHubAutocompleteBarEmptyPreview() {
    GitHubAutocompleteBarComponent(
        uiState = AutocompleteUiState.Empty,
        onQueryChanged = {},
        onRetry = {},
        onItemClick = {},
        onLeadingIconClick = {},
        initialText = "zzz",
        initialActive = true
    )
}

@Preview(name = "Error", showBackground = true, widthDp = 380, heightDp = 320)
@Composable
private fun GitHubAutocompleteBarErrorPreview() {
    GitHubAutocompleteBarComponent(
        uiState = AutocompleteUiState.Error("Check your connection and try again."),
        onQueryChanged = {},
        onRetry = {},
        onItemClick = {},
        onLeadingIconClick = {},
        initialText = "flow",
        initialActive = true
    )
}

@Preview(name = "Idle", showBackground = true, widthDp = 380, heightDp = 120)
@Composable
private fun GitHubAutocompleteBarIdlePreview() {
    GitHubAutocompleteBarComponent(
        uiState = AutocompleteUiState.Idle,
        onQueryChanged = {},
        onRetry = {},
        onItemClick = {},
        onLeadingIconClick = {}
    )
}
