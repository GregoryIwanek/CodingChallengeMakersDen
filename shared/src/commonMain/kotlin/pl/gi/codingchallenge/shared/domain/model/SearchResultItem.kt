package pl.gi.codingchallenge.shared.domain.model

sealed interface SearchResultItem {
    val id: String
    val sortKey: String

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

    data class UserResult(
        override val id: String,
        val login: String,
        val avatarUrl: String?,
        val htmlUrl: String,
    ) : SearchResultItem {
        override val sortKey: String get() = login
    }
}
