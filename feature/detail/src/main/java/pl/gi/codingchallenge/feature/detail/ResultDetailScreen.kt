package pl.gi.codingchallenge.feature.detail

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import java.text.NumberFormat
import pl.gi.codingchallenge.feature.detail.testing.DetailTestTags
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

/**
 * Full-screen details for a tapped search result. Stateless: every action
 * is a callback, so the host (the nav entry) owns intents and clipboard.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ResultDetailScreen(
    item: SearchResultItem,
    onBack: () -> Unit,
    onOpenOnGitHub: () -> Unit,
    onShare: () -> Unit,
    onCopyUrl: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.testTag(DetailTestTags.SCREEN),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when (item) {
                                is SearchResultItem.RepoResult -> R.string.detail_title_repo
                                is SearchResultItem.UserResult -> R.string.detail_title_user
                            }
                        )
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag(DetailTestTags.BACK_BUTTON)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.detail_back)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.detail_content_padding)),
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(R.dimen.detail_section_spacing)
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (item) {
                is SearchResultItem.RepoResult -> RepoDetails(item)
                is SearchResultItem.UserResult -> UserDetails(item)
            }
            DetailActions(
                onOpenOnGitHub = onOpenOnGitHub,
                onShare = onShare,
                onCopyUrl = onCopyUrl
            )
        }
    }
}

@Composable
private fun RepoDetails(item: SearchResultItem.RepoResult) {
    // The letter tile is decorative (the full name follows), so TalkBack
    // shouldn't read a stray initial before the title.
    Box(
        Modifier
            .clearAndSetSemantics {}
            .size(dimensionResource(R.dimen.detail_avatar_size))
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(
                    dimensionResource(R.dimen.detail_repo_avatar_corner_radius)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            item.name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
    DetailTitle(item.fullName)
    DetailSubtitle(stringResource(R.string.detail_repo_owner, item.ownerLogin))
    item.description?.let { description ->
        Text(
            description,
            modifier = Modifier.testTag(DetailTestTags.DESCRIPTION),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
    Row(
        modifier = Modifier.testTag(DetailTestTags.STARS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Star,
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.detail_star_icon_size)),
            tint = MaterialTheme.colorScheme.tertiary
        )
        Spacer(Modifier.width(dimensionResource(R.dimen.detail_icon_text_spacing)))
        Text(
            formatStarsCount(
                resources = LocalResources.current,
                stars = item.stars
            ),
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
private fun UserDetails(item: SearchResultItem.UserResult) {
    Box(
        Modifier
            .size(dimensionResource(R.dimen.detail_avatar_size))
            .background(
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Filled.Person,
            contentDescription = null,
            modifier = Modifier.size(dimensionResource(R.dimen.detail_person_icon_size)),
            tint = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
    DetailTitle(item.login)
    DetailSubtitle(item.htmlUrl.removePrefix("https://"))
}

@Composable
private fun DetailTitle(text: String) {
    Text(
        text,
        modifier = Modifier
            .testTag(DetailTestTags.TITLE)
            .semantics { heading() },
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DetailSubtitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DetailActions(onOpenOnGitHub: () -> Unit, onShare: () -> Unit, onCopyUrl: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(R.dimen.detail_actions_spacing)
        )
    ) {
        ActionButton(
            onClick = onOpenOnGitHub,
            icon = painterResource(R.drawable.ic_open_in_new),
            label = stringResource(R.string.detail_open_on_github),
            testTag = DetailTestTags.OPEN_BUTTON,
            primary = true
        )
        ActionButton(
            onClick = onShare,
            icon = rememberVectorPainter(Icons.Filled.Share),
            label = stringResource(R.string.detail_share),
            testTag = DetailTestTags.SHARE_BUTTON
        )
        ActionButton(
            onClick = onCopyUrl,
            icon = painterResource(R.drawable.ic_content_copy),
            label = stringResource(R.string.detail_copy_url),
            testTag = DetailTestTags.COPY_BUTTON
        )
    }
}

/** Full-width icon + label button; [primary] picks filled over outlined. */
@Composable
private fun ActionButton(
    onClick: () -> Unit,
    icon: Painter,
    label: String,
    testTag: String,
    primary: Boolean = false
) {
    val modifier: Modifier = Modifier
        .fillMaxWidth()
        .testTag(testTag)
    if (primary) {
        Button(onClick = onClick, modifier = modifier) { ButtonContent(icon = icon, label = label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            ButtonContent(icon = icon, label = label)
        }
    }
}

@Composable
private fun ButtonContent(icon: Painter, label: String) {
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(dimensionResource(R.dimen.detail_button_icon_size))
    )
    Spacer(Modifier.width(dimensionResource(R.dimen.detail_icon_text_spacing)))
    Text(label)
}

/**
 * "1 star" / "2,100 stars". Takes [Resources] rather than being @Composable
 * so a JVM test can check the real plurals via Paparazzi's context.
 */
internal fun formatStarsCount(resources: Resources, stars: Int): String =
    resources.getQuantityString(
        R.plurals.detail_stars_count,
        stars,
        // The resources' locale, not the JVM default, so digit grouping matches
        // the language the plural itself is shown in.
        NumberFormat.getIntegerInstance(resources.configuration.locales[0]).format(stars)
    )

@Preview(name = "Repository", showBackground = true)
@Composable
private fun ResultDetailScreenRepoPreview() {
    MaterialTheme {
        ResultDetailScreen(
            item = SearchResultItem.RepoResult(
                id = "1",
                name = "kotlin",
                fullName = "JetBrains/kotlin",
                ownerLogin = "JetBrains",
                avatarUrl = null,
                description = "The Kotlin Programming Language",
                stars = 48000
            ),
            onBack = {},
            onOpenOnGitHub = {},
            onShare = {},
            onCopyUrl = {}
        )
    }
}

@Preview(name = "User", showBackground = true)
@Composable
private fun ResultDetailScreenUserPreview() {
    MaterialTheme {
        ResultDetailScreen(
            item = SearchResultItem.UserResult(
                id = "2",
                login = "octocat",
                avatarUrl = null,
                htmlUrl = "https://github.com/octocat"
            ),
            onBack = {},
            onOpenOnGitHub = {},
            onShare = {},
            onCopyUrl = {}
        )
    }
}
