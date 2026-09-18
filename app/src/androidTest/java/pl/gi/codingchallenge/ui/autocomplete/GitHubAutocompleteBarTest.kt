package pl.gi.codingchallenge.ui.autocomplete

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import androidx.test.espresso.Espresso
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.ui.autocomplete.testing.AutocompleteTestTags

/**
 * Exercises the internal stateless [GitHubAutocompleteBarComponent] overload directly
 * with canned [AutocompleteUiState] values (same approach as the @Previews),
 * so these tests verify real Compose rendering/interaction without going
 * through Hilt or the network. See IMPROVEMENTS.md for the tradeoff.
 */
class GitHubAutocompleteBarTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val sampleResults = listOf(
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

    @Test
    fun idleState_showsNoLoadingEmptyOrError() {
        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.LOADING_INDICATOR).assertDoesNotExist()
        composeRule.onNodeWithTag(AutocompleteTestTags.EMPTY_STATE).assertDoesNotExist()
        composeRule.onNodeWithTag(AutocompleteTestTags.ERROR_STATE).assertDoesNotExist()
        // No empty floating card either — the panel itself shouldn't render.
        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()
    }

    @Test
    fun loadingState_showsLoadingIndicator() {
        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Loading,
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.LOADING_INDICATOR).assertExists()
    }

    @Test
    fun emptyState_showsEmptyMessage() {
        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Empty,
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.EMPTY_STATE).assertExists()
        composeRule.onNodeWithText("No matching users or repositories").assertIsDisplayed()
    }

    @Test
    fun errorState_showsMessage_andRetryButtonInvokesOnRetry() {
        var retryInvoked = false

        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Error("Check your connection and try again."),
                onQueryChanged = {},
                onRetry = { retryInvoked = true },
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "kot",
                initialActive = true
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.ERROR_STATE).assertExists()
        composeRule.onNodeWithText("Check your connection and try again.").assertIsDisplayed()

        composeRule.onNodeWithTag(AutocompleteTestTags.RETRY_BUTTON).performClick()

        assertEquals(true, retryInvoked)
    }

    @Test
    fun successState_showsResultRows_andTappingRowInvokesOnItemClick() {
        var clicked: SearchResultItem? = null

        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = { clicked = it },
                onLeadingIconClick = {},
                initialActive = true
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.resultRow("1")).assertExists()
        composeRule.onNodeWithTag(AutocompleteTestTags.resultRow("2")).assertExists()

        composeRule.onNodeWithTag(AutocompleteTestTags.resultRow("2")).performClick()

        assertEquals(sampleResults[1], clicked)
    }

    @Test
    fun successState_showsDividerBetweenRows_butNotAfterTheLastOne() {
        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true
            )
        }

        // sampleResults has 2 items, so exactly 1 divider between them.
        composeRule.onNodeWithTag(AutocompleteTestTags.resultDivider(0)).assertExists()
        composeRule.onNodeWithTag(AutocompleteTestTags.resultDivider(1)).assertDoesNotExist()
    }

    @Test
    fun successState_resetsScrollPosition_whenTheQueryTextChanges() {
        val firstBatch = List(20) {
            SearchResultItem.RepoResult(
                id = "first-$it",
                name = "first-repo-$it",
                fullName = "owner/first-repo-$it",
                ownerLogin = "owner",
                avatarUrl = null,
                description = null,
                stars = 0
            )
        }
        val secondBatch = List(20) {
            SearchResultItem.RepoResult(
                id = "second-$it",
                name = "second-repo-$it",
                fullName = "owner/second-repo-$it",
                ownerLogin = "owner",
                avatarUrl = null,
                description = null,
                stars = 0
            )
        }

        composeRule.setContent {
            // Stands in for the ViewModel/use case: as soon as the query
            // text changes, "results" for the new query are available
            // immediately (no real debounce/network in this test).
            var uiState by remember {
                mutableStateOf<AutocompleteUiState>(AutocompleteUiState.Success(firstBatch))
            }

            GitHubAutocompleteBarComponent(
                uiState = uiState,
                onQueryChanged = { uiState = AutocompleteUiState.Success(secondBatch) },
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "kot",
                initialActive = true
            )
        }

        // Scroll partway through the first query's results.
        composeRule.onNodeWithTag(AutocompleteTestTags.RESULTS_LIST).performScrollToIndex(15)
        composeRule.onNodeWithTag(AutocompleteTestTags.resultRow("first-0")).assertDoesNotExist()

        // Type a genuinely new query — text changes, new results arrive.
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performTextClearance()
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performTextInput("flow")

        // Fresh results should start scrolled to the top.
        composeRule.onNodeWithTag(AutocompleteTestTags.resultRow("second-0")).assertExists()
    }

    @Test
    fun successState_capsSuggestionPanelHeight_evenWithManyResults() {
        val manyResults = List(50) {
            SearchResultItem.RepoResult(
                id = "$it",
                name = "repo-$it",
                fullName = "owner/repo-$it",
                ownerLogin = "owner",
                avatarUrl = null,
                description = null,
                stars = 0
            )
        }

        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(manyResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true
            )
        }

        // 50 rows would run to well over 1000dp uncapped, so a measured
        // height of exactly 500dp proves the cap — not just that
        // dimens.xml has a value — actually constrains layout.
        composeRule.onNodeWithTag(
            AutocompleteTestTags.SUGGESTION_PANEL
        ).assertHeightIsEqualTo(500.dp)
    }

    @Test
    fun successState_showsFallbackAvatarInitial_whenRepoNameIsEmpty() {
        val emptyNameRepo = SearchResultItem.RepoResult(
            id = "1",
            name = "",
            fullName = "owner/",
            ownerLogin = "owner",
            avatarUrl = null,
            description = null,
            stars = 0
        )

        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(listOf(emptyNameRepo)),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true
            )
        }

        // Doesn't crash on name.first(), and falls back to "?" instead.
        composeRule.onNodeWithText("?").assertIsDisplayed()
    }

    @Test
    fun successState_displaysRepoAndUserContentCorrectly() {
        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialActive = true
            )
        }

        // RepoResult row: name, "owner · description" subtitle, formatted star count.
        composeRule.onNodeWithText("kotlin").assertIsDisplayed()
        composeRule.onNodeWithText(
            "JetBrains · The Kotlin Programming Language"
        ).assertIsDisplayed()
        composeRule.onNodeWithText("48.0k").assertIsDisplayed()

        // UserResult row: login, URL with the scheme stripped.
        composeRule.onNodeWithText("octocat").assertIsDisplayed()
        composeRule.onNodeWithText("github.com/octocat").assertIsDisplayed()
    }

    @Test
    fun typing_forwardsEachCharacterToOnQueryChanged_andExpandsPanel() {
        val queries = mutableListOf<String>()

        // A non-Idle canned state, since focusing/typing alone no longer
        // shows the panel — it only renders once there's real content to
        // show (see idleState_showsNoLoadingEmptyOrError).
        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Loading,
                onQueryChanged = { queries.add(it) },
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {}
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
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = { queries.add(it) },
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "kotlin",
                initialActive = true
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.CLEAR_BUTTON).performClick()

        assertEquals("", queries.last())
        // Clearing doesn't collapse the panel (active stays true, so the
        // field is ready for retyping) but Idle content means no empty
        // floating card should be left behind either.
        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()
    }

    @Test
    fun leadingIcon_collapsesActivePanel_andInvokesCallback() {
        var callbackInvoked = false

        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = { callbackInvoked = true },
                initialText = "kot",
                initialActive = true
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
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = { callbackInvoked = true },
                initialText = "kot",
                initialActive = false
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
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "kot",
                initialActive = true
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
    fun systemBack_collapsesActivePanel_andFieldTapReopensIt() {
        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "kot",
                initialActive = true
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertExists()

        Espresso.pressBack()

        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()

        // Back also clears focus (like the leading icon), so a plain field
        // tap — not another back press — is what reopens the panel.
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performClick()
        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertExists()
    }

    @Test
    fun systemBack_doesNothing_whenPanelNotActive() {
        var outerBackInvoked = false

        composeRule.setContent {
            // Only fires if our BackHandler(enabled = false) correctly
            // declines to consume the back event.
            BackHandler(enabled = true) { outerBackInvoked = true }

            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {}
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()

        Espresso.pressBack()

        assertEquals(true, outerBackInvoked)
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).assertExists()
        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()
    }

    @Test
    fun leadingIcon_isNeutralAndDisabled_whenQueryTooShortForSuggestions() {
        var callbackInvoked = false

        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = { callbackInvoked = true },
                initialText = "ko" // < 3 chars: still Idle, nothing to show
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertExists()
        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertIsNotEnabled()
        composeRule.onNodeWithContentDescription("Collapse suggestions").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Expand suggestions").assertDoesNotExist()

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).performClick()

        assertEquals(false, callbackInvoked)
        composeRule.onNodeWithTag(AutocompleteTestTags.SUGGESTION_PANEL).assertDoesNotExist()
    }

    @Test
    fun leadingIcon_isNeutralAndDisabled_whenTextEmpty() {
        composeRule.setContent {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {}
            )
        }

        // Visible from the start now — a neutral search glyph, not gated
        // on having text.
        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertExists()
        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun leadingIcon_becomesEnabledWhenSuggestionsAppear_andDisabledAgainWhenCleared() {
        composeRule.setContent {
            // Stands in for the ViewModel: 3+ chars means there's
            // something to show, matching MIN_QUERY_LENGTH in
            // SearchAutocompleteUseCase.
            var uiState by remember {
                mutableStateOf<AutocompleteUiState>(AutocompleteUiState.Idle)
            }

            GitHubAutocompleteBarComponent(
                uiState = uiState,
                onQueryChanged = {
                    uiState = if (it.length >= 3) {
                        AutocompleteUiState.Success(sampleResults)
                    } else {
                        AutocompleteUiState.Idle
                    }
                },
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {}
            )
        }

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertIsNotEnabled()

        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performClick()
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performTextInput("kot")

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertIsEnabled()

        composeRule.onNodeWithTag(AutocompleteTestTags.CLEAR_BUTTON).performClick()

        composeRule.onNodeWithTag(AutocompleteTestTags.LEADING_ICON_BUTTON).assertIsNotEnabled()
    }
}
