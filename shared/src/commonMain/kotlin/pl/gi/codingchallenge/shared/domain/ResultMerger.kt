package pl.gi.codingchallenge.shared.domain

import pl.gi.codingchallenge.shared.domain.model.MAX_RESULTS
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

fun mergeAndSort(
    users: List<SearchResultItem.UserResult>,
    repos: List<SearchResultItem.RepoResult>,
    limit: Int = MAX_RESULTS
): List<SearchResultItem> = (users + repos)
    // Avoids recomputing .lowercase() inside the comparator (sortedBy
    // re-invokes its selector on every comparison, not once per item).
    // Negligible at 50 items — this is about not shipping the
    // anti-pattern, not a real perf win here.
    .map { it to it.sortKey.lowercase() }
    .sortedBy { (_, lowercaseKey) -> lowercaseKey }
    .map { (item, _) -> item }
    .take(limit)
