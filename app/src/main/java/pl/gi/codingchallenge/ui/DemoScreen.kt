package pl.gi.codingchallenge.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.gi.codingchallenge.domain.model.SearchResultItem
import pl.gi.codingchallenge.ui.autocomplete.GitHubAutocompleteBarComponent

/**
 * Hosts GitHubAutocompleteBar over two unrelated backdrops, switchable
 * with the segmented control at the top — proves the component floats
 * over arbitrary host content rather than owning a screen of its own
 */
@Composable
fun DemoScreen() {
    var selectedBackdrop by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    Box(Modifier.fillMaxSize()) {
        when (selectedBackdrop) {
            0 -> MapBackdrop(Modifier.fillMaxSize())
            else -> DashboardBackdrop(Modifier.fillMaxSize())
        }

        // statusBarsPadding() keeps both the toggle and the bar clear of the
        // status bar — content up there can have its touches swallowed by
        // system gesture handling (confirmed on-device, not just an ADB
        // testing artifact), and MainActivity doesn't otherwise handle insets.
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BackdropToggle("Map", selected = selectedBackdrop == 0) { selectedBackdrop = 0 }
                BackdropToggle("Dashboard", selected = selectedBackdrop == 1) { selectedBackdrop = 1 }
            }

            Spacer(Modifier.height(12.dp))

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
        }
    }
}

@Composable
private fun BackdropToggle(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color(0xFFE9DDFB) else Color.Transparent,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = Color(0xFF1F2328),
        )
    }
}

@Composable
private fun MapBackdrop(modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFFEAF3EC))) {
        Box(
            Modifier
                .size(width = 260.dp, height = 180.dp)
                .offset(x = 90.dp, y = 220.dp)
                .rotate(-12f)
                .background(Color(0xFFCFE8FA), RoundedCornerShape(44)),
        )
        Box(
            Modifier
                .size(width = 220.dp, height = 160.dp)
                .offset(x = (-60).dp, y = 480.dp)
                .rotate(18f)
                .background(Color(0xFFDCEFDD), RoundedCornerShape(44)),
        )
        Box(
            Modifier
                .size(width = 180.dp, height = 140.dp)
                .offset(x = 190.dp, y = 560.dp)
                .rotate(-8f)
                .background(Color(0xFFDCEFDD), RoundedCornerShape(44)),
        )
    }
}

@Composable
private fun DashboardBackdrop(modifier: Modifier = Modifier) {
    Column(modifier.background(Color(0xFFF6F8FA))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp, 200.dp, 16.dp, 16.dp),
        ) {
            Box(Modifier.size(40.dp).background(Color(0xFF0969DA), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Hi, Alex", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Here’s what’s happening", fontSize = 12.sp, color = Color(0xFF59636E))
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardCard("Recent Activity")
            DashboardCard("Pinned Projects")
        }
    }
}

@Composable
private fun DashboardCard(title: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1F2328))
        Spacer(Modifier.height(10.dp))
        Row {
            Box(Modifier.size(28.dp).background(Color(0xFFEDF1F4), CircleShape))
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    Modifier
                        .width(120.dp)
                        .height(8.dp)
                        .background(Color(0xFFEDF1F4), RoundedCornerShape(4.dp)),
                )
                Box(
                    Modifier
                        .width(80.dp)
                        .height(8.dp)
                        .background(Color(0xFFEDF1F4), RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

// DemoScreen() itself isn't previewed: its default `viewModel = hiltViewModel()`
// doesn't resolve in @Preview. These preview the Hilt-free pieces it's built from.

@Preview(name = "Map backdrop", showBackground = true, widthDp = 380, heightDp = 700)
@Composable
private fun MapBackdropPreview() {
    MapBackdrop(Modifier.fillMaxSize())
}

@Preview(name = "Dashboard backdrop", showBackground = true, widthDp = 380, heightDp = 700)
@Composable
private fun DashboardBackdropPreview() {
    DashboardBackdrop(Modifier.fillMaxSize())
}

@Preview(name = "Toggle", showBackground = true, widthDp = 220, heightDp = 60)
@Composable
private fun BackdropTogglePreview() {
    Row(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BackdropToggle("Map", selected = true) {}
        BackdropToggle("Dashboard", selected = false) {}
    }
}
