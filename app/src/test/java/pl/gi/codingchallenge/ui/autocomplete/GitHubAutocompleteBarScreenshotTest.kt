package pl.gi.codingchallenge.ui.autocomplete

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.android.ide.common.rendering.api.SessionParams
import org.junit.Rule
import org.junit.Test
import pl.gi.codingchallenge.domain.AutocompleteUiState
import pl.gi.codingchallenge.domain.model.SearchResultItem

/**
 * Golden-image tests for GitHubAutocompleteBarComponent's five UI states —
 * same canned states already used by the @Previews in
 * GitHubAutocompleteBar.kt. `./gradlew :app:recordPaparazziDebug`
 * (re)generates goldens; `:app:verifyPaparazziDebug` checks against them.
 */
class GitHubAutocompleteBarScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6.copy(softButtons = false, screenHeight = 1),
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
        theme = "Theme.CodingChallenge",
    )

    private val sampleResults = listOf(
        SearchResultItem.RepoResult(
            id = "1", name = "dataflow-kt", fullName = "kotlinx/dataflow-kt", ownerLogin = "kotlinx",
            avatarUrl = null, description = "Structured concurrency data pipelines", stars = 2100,
        ),
        SearchResultItem.UserResult(
            id = "2", login = "flowdev", avatarUrl = null, htmlUrl = "https://github.com/flowdev",
        ),
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
                initialActive = true,
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
                initialActive = true,
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
                initialActive = true,
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
                initialActive = true,
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
                onLeadingIconClick = {},
            )
        }
    }
}
