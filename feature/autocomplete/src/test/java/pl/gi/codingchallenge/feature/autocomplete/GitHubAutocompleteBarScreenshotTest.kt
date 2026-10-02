package pl.gi.codingchallenge.feature.autocomplete

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

/**
 * Golden-image tests for GitHubAutocompleteBarComponent's five UI states —
 * same canned states already used by the @Previews in
 * GitHubAutocompleteBar.kt — plus the singular and capped result-count
 * header variants. `./gradlew :feature:autocomplete:recordPaparazziDebug`
 * (re)generates goldens; `:feature:autocomplete:verifyPaparazziDebug` checks against them.
 */
class GitHubAutocompleteBarScreenshotTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        // Pinned so goldens don't drift between machines (locale otherwise
        // reads a JVM system property from the host).
        deviceConfig = DeviceConfig.PIXEL_6.copy(fontScale = 1f, locale = "en"),
        renderingMode = SessionParams.RenderingMode.SHRINK,
        // Same platform theme :app uses (Theme.CodingChallenge's parent).
        theme = "android:Theme.Material.Light.NoActionBar",
        // Small tolerance for cross-OS antialiasing; unverified, no CI yet.
        maxPercentDifference = 0.1
    )

    private val sampleResults: List<SearchResultItem> = listOf(
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
        )
    )

    @Test
    fun success() {
        paparazzi.snapshot {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(sampleResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "flow",
                initialActive = true
            )
        }
    }

    @Test
    fun oneResult() {
        paparazzi.snapshot {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(sampleResults.take(1)),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "flow",
                initialActive = true
            )
        }
    }

    @Test
    fun cappedResults() {
        val cappedResults: List<SearchResultItem> = List(DEFAULT_MAX_RESULTS) {
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
        paparazzi.snapshot {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Success(cappedResults),
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {},
                initialText = "repo",
                initialActive = true
            )
        }
    }

    @Test
    fun loading() {
        paparazzi.snapshot {
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
    }

    @Test
    fun empty() {
        paparazzi.snapshot {
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
    }

    @Test
    fun error() {
        paparazzi.snapshot {
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
    }

    @Test
    fun idle() {
        // No panel renders for Idle (see GitHubAutocompleteBar.kt) — this
        // golden is just the bare search bar.
        paparazzi.snapshot {
            GitHubAutocompleteBarComponent(
                uiState = AutocompleteUiState.Idle,
                onQueryChanged = {},
                onRetry = {},
                onItemClick = {},
                onLeadingIconClick = {}
            )
        }
    }
}
