package pl.gi.codingchallenge.ui.autocomplete

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.gi.codingchallenge.domain.AutocompleteUiState
import pl.gi.codingchallenge.domain.model.SearchResultItem

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
fun GitHubAutocompleteBar(
    modifier: Modifier = Modifier,
    viewModel: AutocompleteViewModel = hiltViewModel(),
    onItemClick: (SearchResultItem) -> Unit = {},
    onLeadingIconClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    GitHubAutocompleteBar(
        uiState = uiState,
        onQueryChanged = viewModel::onQueryChanged,
        onItemClick = onItemClick,
        onLeadingIconClick = onLeadingIconClick,
        modifier = modifier,
    )
}

/**
 * Stateless implementation — hoisted out so it can be previewed/tested
 * without Hilt. `internal` (not `private`) so androidTest, which is
 * compiled as part of this same module, can exercise it directly with
 * canned [AutocompleteUiState] values instead of going through Hilt.
 */
@Composable
internal fun GitHubAutocompleteBar(
    uiState: AutocompleteUiState,
    onQueryChanged: (String) -> Unit,
    onItemClick: (SearchResultItem) -> Unit,
    onLeadingIconClick: () -> Unit,
    modifier: Modifier = Modifier,
    initialText: String = "",
    initialActive: Boolean = false,
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    var active by rememberSaveable { mutableStateOf(initialActive) }

    Column(modifier) {
        FloatingSearchBar(
            text = text,
            active = active,
            onTextChange = {
                text = it
                onQueryChanged(it)
            },
            onActiveChange = { active = it },
            onLeadingIconClick = onLeadingIconClick,
        )

        if (active) {
            Spacer(Modifier.height(10.dp))
            SuggestionPanel {
                when (uiState) {
                    AutocompleteUiState.Idle -> {} // < 3 chars: host content stays visible behind the bar
                    AutocompleteUiState.Loading -> LoadingIndicator()
                    AutocompleteUiState.Empty -> EmptyState()
                    is AutocompleteUiState.Error -> ErrorState(uiState.message) {
                        onQueryChanged(text) // retry
                    }

                    is AutocompleteUiState.Success -> LazyColumn {
                        items(uiState.items, key = { it.id }) { item ->
                            SearchResultRow(item, onClick = { onItemClick(item) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingSearchBar(
    text: String,
    active: Boolean,
    onTextChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onLeadingIconClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(6.dp, RoundedCornerShape(26.dp))
            .background(AutocompleteColors.Surface, RoundedCornerShape(26.dp))
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = {
                if (active) onActiveChange(false)
                onLeadingIconClick()
            },
            modifier = Modifier.testTag(AutocompleteTestTags.LEADING_ICON_BUTTON),
        ) {
            Icon(
                imageVector = if (active) Icons.AutoMirrored.Filled.ArrowBack else Icons.Filled.Menu,
                contentDescription = if (active) "Back" else "Menu",
                tint = AutocompleteColors.TextSecondary,
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            if (text.isEmpty()) {
                Text(
                    text = "Search users & repositories",
                    style = TextStyle(fontSize = 15.sp, color = AutocompleteColors.TextSecondary),
                )
            }
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = AutocompleteColors.TextPrimary),
                cursorBrush = SolidColor(AutocompleteColors.Accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(AutocompleteTestTags.SEARCH_FIELD)
                    .onFocusChanged { if (it.isFocused) onActiveChange(true) },
            )
        }

        if (text.isNotEmpty()) {
            IconButton(
                onClick = { onTextChange("") },
                modifier = Modifier.size(32.dp).testTag(AutocompleteTestTags.CLEAR_BUTTON),
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(AutocompleteColors.Border, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Clear,
                        contentDescription = "Clear",
                        tint = AutocompleteColors.TextSecondary,
                        modifier = Modifier.size(11.dp),
                    )
                }
            }
        } else {
            Box(Modifier.width(6.dp))
        }
    }
}

@Composable
private fun SuggestionPanel(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.SUGGESTION_PANEL)
            .shadow(14.dp, RoundedCornerShape(20.dp))
            .background(AutocompleteColors.Surface, RoundedCornerShape(20.dp)),
    ) {
        content()
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.LOADING_INDICATOR)
            .height(150.dp), contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = AutocompleteColors.Accent,
            strokeWidth = 3.dp,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.EMPTY_STATE)
            .height(150.dp), contentAlignment = Alignment.Center
    ) {
        Text(
            "No matching users or repositories",
            fontSize = 13.sp,
            color = AutocompleteColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.ERROR_STATE)
            .height(180.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Filled.Warning,
                contentDescription = null,
                tint = AutocompleteColors.ErrorColor,
                modifier = Modifier.size(28.dp)
            )
            Text(
                "Couldn’t load results",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AutocompleteColors.TextPrimary
            )
            Text(
                message,
                fontSize = 11.sp,
                color = AutocompleteColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(2.dp))
            OutlinedButton(
                onClick = onRetry,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag(AutocompleteTestTags.RETRY_BUTTON),
            ) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = AutocompleteColors.Accent,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    "Retry",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AutocompleteColors.Accent
                )
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: SearchResultItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.resultRow(item.id))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (item) {
            is SearchResultItem.RepoResult -> {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(AutocompleteColors.RepoBg, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.name.first().uppercase(),
                        color = AutocompleteColors.Accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AutocompleteColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.description?.let { "${item.ownerLogin} · $it" } ?: item.ownerLogin,
                        fontSize = 12.sp,
                        color = AutocompleteColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = AutocompleteColors.StarColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        formatStars(item.stars),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AutocompleteColors.StarColor
                    )
                }
            }

            is SearchResultItem.UserResult -> {
                Box(
                    Modifier
                        .size(34.dp)
                        .background(AutocompleteColors.UserBg, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = AutocompleteColors.UserIcon,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.login,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = AutocompleteColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.htmlUrl.removePrefix("https://"),
                        fontSize = 12.sp,
                        color = AutocompleteColors.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun formatStars(count: Int): String =
    if (count >= 1000) "${count / 1000}.${(count % 1000) / 100}k" else count.toString()

// ---- Previews -------------------------------------------------------
// GitHubAutocompleteBar's public overload needs Hilt (hiltViewModel()),
// which doesn't resolve in @Preview — these call the private stateless
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
    GitHubAutocompleteBar(
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
    GitHubAutocompleteBar(
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
    GitHubAutocompleteBar(
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
    GitHubAutocompleteBar(
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
    GitHubAutocompleteBar(
        uiState = AutocompleteUiState.Idle,
        onQueryChanged = {},
        onItemClick = {},
        onLeadingIconClick = {},
    )
}
