package pl.gi.codingchallenge.feature.detail

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import pl.gi.codingchallenge.feature.detail.testing.DetailTestTags
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

/**
 * Exercises the module's public nav API (navigateToDetail + detailScreen)
 * inside a throwaway NavHost, standing in for the app's real graph.
 */
class DetailNavigationTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Serializable
    private data object StartRoute

    private val repo = SearchResultItem.RepoResult(
        id = "1",
        name = "kotlin",
        fullName = "JetBrains/kotlin",
        ownerLogin = "JetBrains",
        avatarUrl = null,
        description = "The Kotlin Programming Language",
        stars = 48000
    )

    @Test
    fun navigateToDetail_showsItem_andBackReturnsToStart() {
        composeRule.setContent {
            val navController: NavHostController = rememberNavController()
            MaterialTheme {
                NavHost(navController = navController, startDestination = StartRoute) {
                    composable<StartRoute> {
                        Button(
                            onClick = { navController.navigateToDetail(repo) },
                            modifier = Modifier.testTag(START_BUTTON)
                        ) {
                            Text("Open")
                        }
                    }
                    detailScreen(onBack = navController::popBackStack)
                }
            }
        }

        composeRule.onNodeWithTag(START_BUTTON).performClick()
        composeRule.onNodeWithTag(DetailTestTags.SCREEN).assertIsDisplayed()
        composeRule.onNodeWithTag(DetailTestTags.TITLE).assertTextEquals("JetBrains/kotlin")

        composeRule.onNodeWithTag(DetailTestTags.BACK_BUTTON).performClick()
        composeRule.onNodeWithTag(START_BUTTON).assertIsDisplayed()
        composeRule.onNodeWithTag(DetailTestTags.SCREEN).assertDoesNotExist()
    }

    private companion object {
        const val START_BUTTON: String = "start_button"
    }
}
