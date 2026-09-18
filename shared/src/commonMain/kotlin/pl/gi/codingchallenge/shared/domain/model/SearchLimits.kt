package pl.gi.codingchallenge.shared.domain.model

/**
 * Single source of truth for the "50" result cap, shared by
 * [GitHubSearchRepository]'s `perTypeLimit` default and [mergeAndSort]'s
 * `limit` default. `perTypeLimit` must be >= this: if the true top
 * [MAX_RESULTS] alphabetically-sorted results turned out to all be one
 * type (e.g. all users), fetching fewer than [MAX_RESULTS] of that type
 * would silently drop some of them before merging ever runs.
 *
 * AT-14: that guarantee only holds *within whatever candidate pool
 * GitHubApi actually fetches*. GitHubApi never sends a `sort` param, so
 * that pool is GitHub's own relevance-ranked top-[MAX_RESULTS] per type,
 * not a true alphabetical one - for a query with more than [MAX_RESULTS]
 * matches of one type, an alphabetically-earlier match can rank outside
 * GitHub's relevance top-[MAX_RESULTS] and never reach [mergeAndSort] at
 * all.
 */
internal const val MAX_RESULTS: Int = 50
