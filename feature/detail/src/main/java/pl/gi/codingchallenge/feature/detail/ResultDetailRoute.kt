package pl.gi.codingchallenge.feature.detail

import android.content.Context
import android.os.Build
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

/**
 * Stateful wrapper around [ResultDetailScreen]: wires its buttons to the
 * browser, share sheet and clipboard, so the screen itself stays testable.
 */
@Composable
internal fun ResultDetailRoute(
    item: SearchResultItem,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context: Context = LocalContext.current
    val scope: CoroutineScope = rememberCoroutineScope()
    val snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
    val copiedMessage: String = stringResource(R.string.detail_url_copied)
    val url: String = remember(item) { item.gitHubUrl() }

    ResultDetailScreen(
        item = item,
        onBack = onBack,
        onOpenOnGitHub = { openInBrowser(context = context, url = url) },
        onShare = { shareLink(context = context, url = url) },
        onCopyUrl = {
            copyLink(context = context, url = url)
            // Android 13+ shows its own clipboard confirmation; a snackbar
            // on top would duplicate it.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
            }
        },
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}
