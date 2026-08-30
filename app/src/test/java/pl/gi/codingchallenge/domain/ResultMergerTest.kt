package pl.gi.codingchallenge.domain

import org.junit.Assert.assertEquals
import pl.gi.codingchallenge.domain.model.SearchResultItem
import org.junit.Test

class ResultMergerTest {

    @Test
    fun `merges users and repos then sorts case-insensitively by name`() {
        val repos = listOf(repo(name = "banana-cli"), repo(name = "Apple-sdk"))
        val users = listOf(user(login = "avocado"), user(login = "Cherry"))

        val merged = mergeAndSort(users, repos)

        assertEquals(
            listOf("Apple-sdk", "avocado", "banana-cli", "Cherry"),
            merged.map { it.sortKey },
        )
    }

    @Test
    fun `caps combined results at 50`() {
        val repos = List(40) { repo(name = "repo$it") }
        val users = List(40) { user(login = "user$it") }

        assertEquals(50, mergeAndSort(users, repos).size)
    }
}

private fun repo(
    name: String,
    fullName: String = "owner/$name",
    ownerLogin: String = "owner",
    stars: Int = 0,
) = SearchResultItem.RepoResult(
    id = name,
    name = name,
    fullName = fullName,
    ownerLogin = ownerLogin,
    avatarUrl = null,
    description = null,
    stars = stars,
)

private fun user(
    login: String,
) = SearchResultItem.UserResult(
    id = login,
    login = login,
    avatarUrl = null,
    htmlUrl = "https://github.com/$login",
)
