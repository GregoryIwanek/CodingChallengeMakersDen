package pl.gi.codingchallenge.ui.autocomplete

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import pl.gi.codingchallenge.domain.AutocompleteUiState
import pl.gi.codingchallenge.domain.model.SearchResultItem

// GitHubAutocompleteBar's public overload needs Hilt (hiltViewModel()),
// which doesn't resolve in @Preview — these call the internal stateless
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
    GitHubAutocompleteBarComponent(
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
    GitHubAutocompleteBarComponent(
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
    GitHubAutocompleteBarComponent(
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
    GitHubAutocompleteBarComponent(
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
    GitHubAutocompleteBarComponent(
        uiState = AutocompleteUiState.Idle,
        onQueryChanged = {},
        onItemClick = {},
        onLeadingIconClick = {},
    )
}
