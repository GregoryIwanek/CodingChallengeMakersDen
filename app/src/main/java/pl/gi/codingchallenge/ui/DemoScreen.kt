package pl.gi.codingchallenge.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import pl.gi.codingchallenge.domain.model.SearchResultItem
import pl.gi.codingchallenge.ui.autocomplete.GitHubAutocompleteBarComponent

/**
 * Three-tab demo: Overview describes the assignment and requirements; Component
 * hosts the bar with the panel pushing content down; Overlay shows the same bar
 * floating over independent scrolling content instead.
 */
@Composable
fun DemoScreen() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    label = { Text("Overview") },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text("Component") },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("Overlay") },
                )
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            0 -> OverviewScreen(Modifier.padding(innerPadding).fillMaxSize())
            1 -> ComponentScreen(Modifier.padding(innerPadding).fillMaxSize())
            else -> OverlayScreen(Modifier.padding(innerPadding).fillMaxSize())
        }
    }
}

private val requirements = listOf(
    "Search starts only once the query is at least 3 characters long",
    "Searches both GitHub users and repositories",
    "Merges both result types into a single, alphabetically-sorted list",
    "Caps the combined result list at 50 items",
    "Shows loading, empty, and error states",
    "Handles rapid typing gracefully — debounced, cancels stale requests",
    "Works as a reusable component, not hardcoded to one screen",
    "Built without any autocomplete/search library",
)

@Composable
private fun OverviewScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("GitHub Users & Repositories Autocomplete", style = MaterialTheme.typography.headlineSmall)
        Text(
            "A reusable Jetpack Compose component that searches GitHub users and " +
                "repositories as you type, merging both into one alphabetically-sorted list.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Text("Requirements", style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            requirements.forEach { requirement ->
                Text("•  $requirement", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Open the Component tab below to try it live.", style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ComponentScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        GitHubAutocompleteBarComponent(
            modifier = Modifier.fillMaxWidth(),
            onItemClick = { item ->
                val label = when (item) {
                    is SearchResultItem.RepoResult -> item.fullName
                    is SearchResultItem.UserResult -> item.login
                }
                Toast.makeText(context, "Tapped $label", Toast.LENGTH_SHORT).show()
            },
        )
        Text(
            "The suggestion panel above floats over whatever content sits below it — " +
                "it never owns the backdrop, scrim, or navigation. Dropped inside a Box " +
                "aligned over a real screen, the panel would overlay that screen's content " +
                "instead of pushing it down like it does here.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OverlayScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    Box(modifier) {
        MockFeed(Modifier.fillMaxSize())

        GitHubAutocompleteBarComponent(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .fillMaxWidth(),
            onItemClick = { item ->
                val label = when (item) {
                    is SearchResultItem.RepoResult -> item.fullName
                    is SearchResultItem.UserResult -> item.login
                }
                Toast.makeText(context, "Tapped $label", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

@Composable
private fun MockFeed(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 88.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(20) { index -> FeedCard(index) }
    }
}

@Composable
private fun FeedCard(index: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Feed item ${index + 1}", style = MaterialTheme.typography.titleSmall)
        Text(
            "Unrelated host content — the suggestion panel overlays this feed " +
                "instead of pushing it down.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Preview(name = "Overview", showBackground = true, widthDp = 380, heightDp = 700)
@Composable
private fun OverviewScreenPreview() {
    OverviewScreen(Modifier.fillMaxSize())
}

@Preview(name = "Mock feed", showBackground = true, widthDp = 380, heightDp = 700)
@Composable
private fun MockFeedPreview() {
    MockFeed(Modifier.fillMaxSize())
}
