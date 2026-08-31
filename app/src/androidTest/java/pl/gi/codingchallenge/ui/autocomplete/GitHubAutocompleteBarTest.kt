package pl.gi.codingchallenge.ui.autocomplete

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.gi.codingchallenge.domain.AutocompleteUiState
import pl.gi.codingchallenge.domain.model.SearchResultItem

/**
 * Exercises the internal stateless [GitHubAutocompleteBar] overload directly
 * with canned [AutocompleteUiState] values (same approach as the @Previews),
 * so these tests verify real Compose rendering/interaction without going
 * through Hilt or the network. See IMPROVEMENTS.md for the tradeoff.
 */
class GitHubAutocompleteBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleResults = listOf(
        SearchResultItem.RepoResult(
            id = "1", name = "kotlin", fullName = "JetBrains/kotlin", ownerLogin = "JetBrains",
            avatarUrl = null, description = "The Kotlin Programming Language", stars = 48000,
        ),
        SearchResultItem.UserResult(
            id = "2", login = "octocat", avatarUrl = null, htmlUrl = "https://github.com/octocat",
        ),
    )

    @Test
    fun idleState_showsNoLoadingEmptyOrError() {
        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true,
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.LOADING_INDICATOR).assertDoesNotExist()
        composeRule.onNodeWithTag(AutocompleteTestTags.EMPTY_STATE).assertDoesNotExist()
        composeRule.onNodeWithTag(AutocompleteTestTags.ERROR_STATE).assertDoesNotExist()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Loading,
                onQueryChanged = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true,
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.LOADING_INDICATOR).assertExists()
    }

    @Test
    fun emptyState_showsEmptyMessage() {
        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Empty,
                onQueryChanged = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true,
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithText("No matching users or repositories").assertIsDisplayed()
    }

    @Test
    fun errorState_showsMessage_andRetryInvokesOnQueryChangedWithCurrentText() {
        var retriedWith: String? = null

        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Error("Check your connection and try again."),
                onQueryChanged = { retriedWith = it },
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "kot",
                initialActive = true,
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithText("Check your connection and try again.").assertIsDisplayed()

        composeRule.onNodeWithTag(AutocompleteTestTags.RETRY_BUTTON).performClick()

        assertEquals("kot", retriedWith)
    }

    @Test
    fun successState_showsResultRows_andTappingRowInvokesOnItemClick() {
        var clicked: SearchResultItem? = null

        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onItemClick = { clicked = it },
                onLeadingIconClick = {},
                initialActive = true,
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.resultRow("1")).assertExists()
        composeRule.onNodeWithTag(AutocompleteTestTags.resultRow("2")).assertExists()

        composeRule.onNodeWithTag(AutocompleteTestTags.resultRow("2")).performClick()

        assertEquals(sampleResults[1], clicked)
    }

    @Test
    fun successState_displaysRepoAndUserContentCorrectly() {
        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true,
            )
        }

        // RepoResult row: name, "owner · description" subtitle, formatted star count.
        composeRule.onNodeWithText("kotlin").assertIsDisplayed()
        composeRule.onNodeWithText("JetBrains · The Kotlin Programming Language").assertIsDisplayed()
        composeRule.onNodeWithText("48.0k").assertIsDisplayed()

        // UserResult row: login, URL with the scheme stripped.
        composeRule.onNodeWithText("octocat").assertIsDisplayed()
        composeRule.onNodeWithText("github.com/octocat").assertIsDisplayed()
    }

    @Test
    fun typing_forwardsEachCharacterToOnQueryChanged_andExpandsPanel() {
        val queries = mutableListOf<String>()

        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = { queries.add(it) },
                onItemClick = {},
                onLeadingIconClick = {},
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()

        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performClick()
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performTextInput("kot")

        assertEquals("kot", queries.last())
        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertExists()
    }

    @Test
    fun clearButton_clearsTextAndNotifiesEmptyQuery() {
        val queries = mutableListOf<String>()

        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = { queries.add(it) },
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "kotlin",
                initialActive = true,
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.CLEAR_BUTTON).performClick()

        assertEquals("", queries.last())
    }

    @Test
    fun leadingIcon_collapsesActivePanel_andInvokesCallback() {
        var callbackInvoked = false

        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onItemClick = {},
                onLeadingIconClick = { callbackInvoked = true },
                initialText = "kot",
                initialActive = true,
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertExists()
        composeRule.onNodeWithContentDescription("Collapse suggestions").assertExists()

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).performClick()

        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Expand suggestions").assertExists()
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun leadingIcon_reopensClosedPanel_whenTextPresent() {
        var callbackInvoked = false

        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onItemClick = {},
                onLeadingIconClick = { callbackInvoked = true },
                initialText = "kot",
                initialActive = false,
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Expand suggestions").assertExists()

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).performClick()

        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertExists()
        composeRule.onNodeWithContentDescription("Collapse suggestions").assertExists()
        assertEquals(true, callbackInvoked)
    }

    @Test
    fun tappingField_reopensClosedPanel_afterCollapsingViaIcon() {
        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "kot",
                initialActive = true,
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertExists()

        // Collapse via the leading icon — this also clears focus (see
        // FloatingSearchBar), which is what makes the re-tap below work.
        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).performClick()
        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()

        // Tapping the field itself (not the icon) should reopen it.
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performClick()
        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertExists()
    }

    @Test
    fun leadingIcon_doesNotExist_whenTextEmpty() {
        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = {},
                onItemClick = {},
                onLeadingIconClick = {},
            )
        }

        // Not composed at all — takes no layout space — until there's text.
        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertDoesNotExist()
    }

    @Test
    fun leadingIcon_appearsWhenTyping_andDisappearsWhenCleared() {
        composeRule.setContent {
            GitHubAutocompleteBar(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = {},
                onItemClick = {},
                onLeadingIconClick = {},
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertDoesNotExist()

        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performClick()
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performTextInput("kot")

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertExists()

        composeRule.onNodeWithTag(AutocompleteTestTags.CLEAR_BUTTON).performClick()

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertDoesNotExist()
    }
}
