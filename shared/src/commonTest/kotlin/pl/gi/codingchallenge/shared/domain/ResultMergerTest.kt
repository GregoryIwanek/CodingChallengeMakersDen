package pl.gi.codingchallenge.shared.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

class ResultMergerTest {

    @Test
    fun `merges users and repos then sorts case-insensitively by name`() {
        val repos = listOf(repo(name = "banana-cli"), repo(name = "Apple-sdk"))
        val users = listOf(user(login = "avocado"), user(login = "Cherry"))

        val merged = mergeAndSort(users = users, repos = repos)

        assertEquals(
            listOf("Apple-sdk", "avocado", "banana-cli", "Cherry"),
            merged.map { it.sortKey }
        )
    }

    @Test
    fun `caps combined results at 50`() {
        val repos = List(40) { repo(name = "repo$it") }
        val users = List(40) { user(login = "user$it") }

        assertEquals(50, mergeAndSort(users = users, repos = repos).size)
    }

    @Test
    fun `cap keeps the alphabetically-first items - not an arbitrary subset`() {
        val repos = listOf(repo(name = "Banana"), repo(name = "Cherry"))
        val users = listOf(user(login = "Apple"), user(login = "Date"))

        val merged = mergeAndSort(users = users, repos = repos, limit = 2)

        assertEquals(listOf("Apple", "Banana"), merged.map { it.sortKey })
    }
}

private fun repo(
    name: String,
    fullName: String = "owner/$name",
    ownerLogin: String = "owner",
    stars: Int = 0
) = SearchResultItem.RepoResult(
    id = name,
    name = name,
    fullName = fullName,
    ownerLogin = ownerLogin,
    avatarUrl = null,
    description = null,
    stars = stars
)

private fun user(login: String) = SearchResultItem.UserResult(
    id = login,
    login = login,
    avatarUrl = null,
    htmlUrl = "https://github.com/$login"
)
