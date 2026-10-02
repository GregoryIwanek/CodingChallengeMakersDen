package pl.gi.codingchallenge.feature.detail

import androidx.compose.runtime.remember
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

// The module's public navigation API: the host app wires these into its own
// NavHost, so no other feature module ever depends on this one directly.

fun NavController.navigateToDetail(item: SearchResultItem) {
    navigate(item.toDetailRoute()) {
        // A double tap on a result would otherwise stack two detail screens.
        launchSingleTop = true
    }
}

fun NavGraphBuilder.detailScreen(onBack: () -> Unit) {
    composable<DetailRoute> { entry ->
        val item: SearchResultItem = remember(entry) { entry.toRoute<DetailRoute>().toItem() }
        ResultDetailRoute(
            item = item,
            // Ignores repeat taps while the exit transition runs, which would
            // otherwise pop the screen underneath too.
            onBack = dropUnlessResumed(block = onBack)
        )
    }
}
