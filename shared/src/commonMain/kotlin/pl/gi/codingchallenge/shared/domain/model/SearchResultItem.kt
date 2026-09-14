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

    @Serializable
    data class RepoResult(
        override val id: String,
        val name: String,
        val fullName: String,
        val ownerLogin: String,
        val avatarUrl: String?,
        val description: String?,
        val stars: Int,
    ) : SearchResultItem {
        override val sortKey: String get() = name
    }

    @Serializable
    data class UserResult(
        override val id: String,
        val login: String,
        val avatarUrl: String?,
        val htmlUrl: String,
    ) : SearchResultItem {
        override val sortKey: String get() = login
    }
}
