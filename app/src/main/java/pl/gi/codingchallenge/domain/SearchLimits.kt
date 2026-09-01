package pl.gi.codingchallenge.domain

/**
 * Single source of truth for the "50" result cap, shared by
 * [GitHubSearchRepository]'s `perTypeLimit` default and [mergeAndSort]'s
 * `limit` default. `perTypeLimit` must be >= this: if the true top
 * [MAX_RESULTS] alphabetically-sorted results turned out to all be one
 * type (e.g. all users), fetching fewer than [MAX_RESULTS] of that type
 * would silently drop some of them before merging ever runs.
 */
internal const val MAX_RESULTS = 50
