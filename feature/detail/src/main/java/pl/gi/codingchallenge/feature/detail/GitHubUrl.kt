package pl.gi.codingchallenge.feature.detail

import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

/**
 * The item's public GitHub page. Search results don't carry a repo URL, so
 * it's built from fullName ("owner/name"), which is exactly GitHub's path.
 */
internal fun SearchResultItem.gitHubUrl(): String = when (this) {
    is SearchResultItem.RepoResult -> "https://github.com/$fullName"
    is SearchResultItem.UserResult -> htmlUrl
}
