package pl.gi.codingchallenge.feature.detail

import androidx.compose.material3.MaterialTheme
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

/**
 * Golden images for the detail screen's repo and user layouts.
 * `./gradlew :feature:detail:recordPaparazziDebug` (re)generates them.
 */
class ResultDetailScreenScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        // Pinned so goldens don't drift between machines, as in :app.
        deviceConfig = DeviceConfig.PIXEL_6.copy(fontScale = 1f, locale = "en"),
        // :app's Theme.CodingChallenge isn't visible from this module; the
        // screen takes its colors from the MaterialTheme wrapper anyway.
        theme = "android:Theme.Material.Light.NoActionBar",
        maxPercentDifference = 0.1
    )

    @Test
    fun repo() {
        snapshot(
            SearchResultItem.RepoResult(
                id = "1",
                name = "kotlin",
                fullName = "JetBrains/kotlin",
                ownerLogin = "JetBrains",
                avatarUrl = null,
                description = "The Kotlin Programming Language",
                stars = 48000
            )
        )
    }

    @Test
    fun repoWithoutDescription() {
        snapshot(
            SearchResultItem.RepoResult(
                id = "3",
                name = "dotfiles",
                fullName = "octocat/dotfiles",
                ownerLogin = "octocat",
                avatarUrl = null,
                description = null,
                stars = 1
            )
        )
    }

    @Test
    fun user() {
        snapshot(
            SearchResultItem.UserResult(
                id = "2",
                login = "octocat",
                avatarUrl = null,
                htmlUrl = "https://github.com/octocat"
            )
        )
    }

    private fun snapshot(item: SearchResultItem) {
        paparazzi.snapshot {
            MaterialTheme {
                ResultDetailScreen(
                    item = item,
                    onBack = {},
                    onOpenOnGitHub = {},
                    onShare = {},
                    onCopyUrl = {}
                )
            }
        }
    }
}
