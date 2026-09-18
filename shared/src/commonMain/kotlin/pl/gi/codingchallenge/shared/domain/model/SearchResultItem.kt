package pl.gi.codingchallenge.shared.domain.model

import kotlinx.serialization.Serializable

// @Serializable on the sealed interface itself, and on every implementing class -
// both are required for kotlinx.serialization's polymorphic (de)serialization.
// Added for the persistence cache (stretch goal): SearchResultCache stores this
// list as a JSON blob, so these need to round-trip through kotlinx.serialization.
@Serializable
sealed interface SearchResultItem {
    val id: String
    val sortKey: String

    // id alone isn't unique across subtypes - RepoResult and UserResult each get their id
    // from GitHub's own independent id sequences (repo ids, user ids), so a user and a repo
    // can legitimately share the same numeric id. List/LazyColumn keys (Android's
    // SuggestionPanel, iOS's ContentView) must use this instead of id directly, or a merged
    // list containing such a pair crashes Compose's LazyColumn with "Key ... was already used".
    val uniqueKey: String

    @Serializable
    data class RepoResult(
        override val id: String,
        val name: String,
        val fullName: String,
        val ownerLogin: String,
        val avatarUrl: String?,
        val description: String?,
        val stars: Int
    ) : SearchResultItem {
        override val sortKey: String get() = name
        override val uniqueKey: String get() = "repo:$id"
    }

    @Serializable
    data class UserResult(
        override val id: String,
        val login: String,
        val avatarUrl: String?,
        val htmlUrl: String
    ) : SearchResultItem {
        override val sortKey: String get() = login
        override val uniqueKey: String get() = "user:$id"
    }
}
