package pl.gi.codingchallenge.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import pl.gi.codingchallenge.feature.detail.detailScreen
import pl.gi.codingchallenge.feature.detail.navigateToDetail

@Serializable
private data object DemoRoute

/**
 * The app's only navigation graph. Feature modules expose NavGraphBuilder /
 * NavController extensions; this is the one place that connects them.
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController: NavHostController = rememberNavController()

    NavHost(navController = navController, startDestination = DemoRoute, modifier = modifier) {
        composable<DemoRoute> {
            DemoScreen(
                onItemClick = navController::navigateToDetail,
                modifier = Modifier.fillMaxSize()
            )
        }
        detailScreen(onBack = navController::popBackStack)
    }
}
