package pl.gi.codingchallenge.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import pl.gi.codingchallenge.R
import pl.gi.codingchallenge.domain.model.SearchResultItem
import pl.gi.codingchallenge.ui.autocomplete.GitHubAutocompleteBarComponent
import pl.gi.codingchallenge.util.dimRes
import pl.gi.codingchallenge.util.strRes

private const val COMPONENT_TAB_VIEW_MODEL_KEY = "component_tab"
private const val OVERLAY_TAB_VIEW_MODEL_KEY = "overlay_tab"

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
                    label = { Text(strRes(R.string.demo_nav_overview)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    label = { Text(strRes(R.string.demo_nav_component)) },
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text(strRes(R.string.demo_nav_overlay)) },
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

@Composable
private fun OverviewScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(dimRes(R.dimen.demo_screen_padding)),
        verticalArrangement = Arrangement.spacedBy(dimRes(R.dimen.demo_section_spacing)),
    ) {
        Text(strRes(R.string.demo_headline), style = MaterialTheme.typography.headlineSmall)
        Text(strRes(R.string.demo_description), style = MaterialTheme.typography.bodyLarge)
        Text(strRes(R.string.demo_requirements_title), style = MaterialTheme.typography.titleMedium)
        Column(verticalArrangement = Arrangement.spacedBy(dimRes(R.dimen.demo_requirement_item_spacing))) {
            stringArrayResource(R.array.demo_requirements).forEach { requirement ->
                Text("•  $requirement", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(dimRes(R.dimen.demo_cta_spacer_height)))
        Text(strRes(R.string.demo_cta), style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun ComponentScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .clearFocusOnTap(focusManager)
            .verticalScroll(rememberScrollState())
            .padding(dimRes(R.dimen.demo_screen_padding)),
        verticalArrangement = Arrangement.spacedBy(dimRes(R.dimen.demo_component_content_spacing)),
    ) {
        GitHubAutocompleteBarComponent(
            modifier = Modifier.fillMaxWidth(),
            viewModel = hiltViewModel(key = COMPONENT_TAB_VIEW_MODEL_KEY),
            onItemClick = { showTappedToast(context, it) },
        )
        Text(strRes(R.string.demo_component_panel_explanation), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun OverlayScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    Box(modifier.clearFocusOnTap(focusManager)) {
        MockFeed(Modifier.fillMaxSize())

        GitHubAutocompleteBarComponent(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(dimRes(R.dimen.demo_overlay_bar_margin))
                .fillMaxWidth(),
            viewModel = hiltViewModel(key = OVERLAY_TAB_VIEW_MODEL_KEY),
            onItemClick = { showTappedToast(context, it) },
        )
    }
}

private fun Modifier.clearFocusOnTap(focusManager: FocusManager): Modifier =
    pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }

private fun showTappedToast(context: Context, item: SearchResultItem) {
    val label = when (item) {
        is SearchResultItem.RepoResult -> item.fullName
        is SearchResultItem.UserResult -> item.login
    }
    Toast.makeText(context, context.getString(R.string.demo_tapped_toast, label), Toast.LENGTH_SHORT).show()
}

@Composable
private fun MockFeed(modifier: Modifier = Modifier) {
    val feedContentPadding = dimRes(R.dimen.demo_feed_content_padding)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            top = dimRes(R.dimen.demo_feed_top_clearance),
            start = feedContentPadding,
            end = feedContentPadding,
            bottom = feedContentPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(dimRes(R.dimen.demo_feed_item_spacing)),
    ) {
        items(20) { index -> FeedCard(index) }
    }
}

@Composable
private fun FeedCard(index: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(dimRes(R.dimen.demo_feed_card_corner_radius)),
            )
            .padding(dimRes(R.dimen.demo_feed_card_padding)),
        verticalArrangement = Arrangement.spacedBy(dimRes(R.dimen.demo_feed_card_content_spacing)),
    ) {
        Text(stringResource(R.string.demo_feed_item_title, index + 1), style = MaterialTheme.typography.titleSmall)
        Text(strRes(R.string.demo_feed_item_body), style = MaterialTheme.typography.bodySmall)
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
