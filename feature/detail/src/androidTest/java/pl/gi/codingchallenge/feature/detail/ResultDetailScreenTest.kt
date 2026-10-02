package pl.gi.codingchallenge.feature.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.gi.codingchallenge.feature.detail.testing.DetailTestTags
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

class ResultDetailScreenTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private val repo = SearchResultItem.RepoResult(
        id = "1",
        name = "kotlin",
        fullName = "JetBrains/kotlin",
        ownerLogin = "JetBrains",
        avatarUrl = null,
        description = "The Kotlin Programming Language",
        stars = 48000
    )

    private val user = SearchResultItem.UserResult(
        id = "2",
        login = "octocat",
        avatarUrl = null,
        htmlUrl = "https://github.com/octocat"
    )

    private val clicks: MutableList<String> = mutableListOf()

    private fun setScreen(item: SearchResultItem) {
        composeRule.setContent {
            MaterialTheme {
                ResultDetailScreen(
                    item = item,
                    onBack = { clicks += "back" },
                    onOpenOnGitHub = { clicks += "open" },
                    onShare = { clicks += "share" },
                    onCopyUrl = { clicks += "copy" }
                )
            }
        }
    }

    @Test
    fun repo_showsNameDescriptionAndStars() {
        setScreen(repo)

        composeRule.onNodeWithTag(DetailTestTags.TITLE).assertTextEquals("JetBrains/kotlin")
        composeRule.onNodeWithTag(DetailTestTags.DESCRIPTION).assertIsDisplayed()
        composeRule.onNodeWithTag(DetailTestTags.STARS).assertIsDisplayed()
    }

    @Test
    fun user_showsLoginWithoutRepoOnlyFields() {
        setScreen(user)

        composeRule.onNodeWithTag(DetailTestTags.TITLE).assertTextEquals("octocat")
        composeRule.onNodeWithTag(DetailTestTags.DESCRIPTION).assertDoesNotExist()
        composeRule.onNodeWithTag(DetailTestTags.STARS).assertDoesNotExist()
    }

    @Test
    fun eachButton_invokesItsOwnCallback() {
        setScreen(repo)

        composeRule.onNodeWithTag(DetailTestTags.BACK_BUTTON).performClick()
        composeRule.onNodeWithTag(DetailTestTags.OPEN_BUTTON).performClick()
        composeRule.onNodeWithTag(DetailTestTags.SHARE_BUTTON).performClick()
        composeRule.onNodeWithTag(DetailTestTags.COPY_BUTTON).performClick()

        assertEquals(listOf("back", "open", "share", "copy"), clicks)
    }
}
