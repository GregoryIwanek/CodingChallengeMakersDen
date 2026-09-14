package pl.gi.codingchallenge.ui.autocomplete

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import pl.gi.codingchallenge.R
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.ui.autocomplete.testing.AutocompleteTestTags
import pl.gi.codingchallenge.util.colRes
import pl.gi.codingchallenge.util.dimRes
import pl.gi.codingchallenge.util.spRes

@Composable
internal fun SearchResultRow(item: SearchResultItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(AutocompleteTestTags.resultRow(item.id))
            .clickable(onClick = onClick)
            .padding(
                horizontal = dimRes(R.dimen.autocomplete_row_horizontal_padding),
                vertical = dimRes(R.dimen.autocomplete_row_vertical_padding),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (item) {
            is SearchResultItem.RepoResult -> {
                Box(
                    Modifier
                        .size(dimRes(R.dimen.autocomplete_avatar_size))
                        .background(
                            color = colRes(R.color.autocomplete_repo_bg),
                            shape = RoundedCornerShape(
                                dimRes(R.dimen.autocomplete_repo_avatar_corner_radius),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        item.name.firstOrNull()?.uppercase() ?: "?",
                        color = colRes(R.color.autocomplete_accent),
                        fontWeight = FontWeight.Bold,
                        fontSize = spRes(R.dimen.autocomplete_item_title_text_size)
                    )
                }
                Spacer(Modifier.width(dimRes(R.dimen.autocomplete_avatar_to_text_spacing)))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.name,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = spRes(R.dimen.autocomplete_item_title_text_size),
                        color = colRes(R.color.autocomplete_text_primary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.description?.let { "${item.ownerLogin} · $it" } ?: item.ownerLogin,
                        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                        color = colRes(R.color.autocomplete_text_secondary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = colRes(R.color.autocomplete_star_color),
                        modifier = Modifier.size(dimRes(R.dimen.autocomplete_star_icon_size))
                    )
                    Spacer(Modifier.width(dimRes(R.dimen.autocomplete_star_icon_text_spacing)))
                    Text(
                        formatStars(item.stars),
                        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                        fontWeight = FontWeight.SemiBold,
                        color = colRes(R.color.autocomplete_star_color)
                    )
                }
            }

            is SearchResultItem.UserResult -> {
                Box(
                    Modifier
                        .size(dimRes(R.dimen.autocomplete_avatar_size))
                        .background(color = colRes(R.color.autocomplete_user_bg), shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        tint = colRes(R.color.autocomplete_user_icon),
                        modifier = Modifier.size(dimRes(R.dimen.autocomplete_person_icon_size))
                    )
                }
                Spacer(Modifier.width(dimRes(R.dimen.autocomplete_avatar_to_text_spacing)))
                Column(Modifier.weight(1f)) {
                    Text(
                        item.login,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = spRes(R.dimen.autocomplete_item_title_text_size),
                        color = colRes(R.color.autocomplete_text_primary),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        item.htmlUrl.removePrefix("https://"),
                        fontSize = spRes(R.dimen.autocomplete_small_label_text_size),
                        color = colRes(R.color.autocomplete_text_secondary),
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

@Preview(name = "Repository", showBackground = true, widthDp = 380)
@Composable
private fun SearchResultRowRepoPreview() {
    SearchResultRow(
        item = SearchResultItem.RepoResult(
            id = "1", name = "kotlin", fullName = "JetBrains/kotlin", ownerLogin = "JetBrains",
            avatarUrl = null, description = "The Kotlin Programming Language", stars = 48000,
        ),
        onClick = {},
    )
}

@Preview(name = "User", showBackground = true, widthDp = 380)
@Composable
private fun SearchResultRowUserPreview() {
    SearchResultRow(
        item = SearchResultItem.UserResult(
            id = "2", login = "octocat", avatarUrl = null, htmlUrl = "https://github.com/octocat",
        ),
        onClick = {},
    )
}
