package pl.gi.codingchallenge.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import pl.gi.codingchallenge.MainActivity
import pl.gi.codingchallenge.R
import pl.gi.codingchallenge.di.FakeSearchModule
import pl.gi.codingchallenge.feature.autocomplete.testing.AutocompleteTestTags
import pl.gi.codingchallenge.feature.detail.R as DetailR

/**
 * End-to-end through the real MainActivity and NavHost: search, open a
 * result's detail screen, come back. Results come from FakeSearchModule,
 * so it runs offline and deterministically.
 */
@HiltAndroidTest
class DetailFlowTest {

    @get:Rule(order = 0)
    val hiltRule: HiltAndroidRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity> =
        createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun componentTab_repoDetail_systemBackKeepsQuery() {
        openTab(R.string.demo_nav_component)
        search("kotlin")

        composeRule.onNodeWithTag(
            AutocompleteTestTags.resultRow(FakeSearchModule.REPO.id)
        ).performClick()
        assertDetailShown(title = FakeSearchModule.REPO.fullName)

        Espresso.pressBack()
        assertBackOnTab(R.string.demo_nav_component, query = "kotlin")
    }

    @Test
    fun componentTab_userDetail_toolbarBackKeepsQuery() {
        openTab(R.string.demo_nav_component)
        search("kotlin")

        composeRule.onNodeWithTag(
            AutocompleteTestTags.resultRow(FakeSearchModule.USER.id)
        ).performClick()
        assertDetailShown(title = FakeSearchModule.USER.login)

        composeRule.onNodeWithContentDescription(string(DetailR.string.detail_back)).performClick()
        assertBackOnTab(R.string.demo_nav_component, query = "kotlin")
    }

    @Test
    fun overlayTab_repoDetail_backKeepsTabSelectedAndQuery() {
        openTab(R.string.demo_nav_overlay)
        search("kotlin")

        composeRule.onNodeWithTag(
            AutocompleteTestTags.resultRow(FakeSearchModule.REPO.id)
        ).performClick()
        assertDetailShown(title = FakeSearchModule.REPO.fullName)

        Espresso.pressBack()
        assertBackOnTab(R.string.demo_nav_overlay, query = "kotlin")
    }

    private fun openTab(labelRes: Int) {
        composeRule.onNodeWithText(string(labelRes)).performClick()
    }

    private fun search(query: String) {
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).performTextInput(query)
        // Keeps the IME from covering rows and from eating the first back press.
        Espresso.closeSoftKeyboard()
        // Real debounce in the real use case, so wait rather than assert immediately.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(AutocompleteTestTags.RESULTS_LIST)
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertDetailShown(title: String) {
        composeRule.onNodeWithContentDescription(
            string(DetailR.string.detail_back)
        ).assertIsDisplayed()
        composeRule.onNodeWithText(title).assertIsDisplayed()
        // Full screen: the demo's bottom navigation is gone.
        composeRule.onNodeWithText(string(R.string.demo_nav_component)).assertDoesNotExist()
    }

    private fun assertBackOnTab(labelRes: Int, query: String) {
        composeRule.onNodeWithText(string(labelRes)).assertIsSelected()
        composeRule.onNodeWithTag(AutocompleteTestTags.SEARCH_FIELD).assertTextContains(query)
    }

    private fun string(res: Int): String = composeRule.activity.getString(res)
}
