package pl.gi.codingchallenge.domain

import pl.gi.codingchallenge.domain.model.SearchResultItem

fun mergeAndSort(
    users: List<SearchResultItem.UserResult>,
    repos: List<SearchResultItem.RepoResult>,
    limit: Int = MAX_RESULTS,
): List<SearchResultItem> =
    (users + repos)
        .sortedBy { it.sortKey.lowercase() }
        .take(limit)
