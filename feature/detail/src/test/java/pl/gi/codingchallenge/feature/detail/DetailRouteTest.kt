package pl.gi.codingchallenge.feature.detail

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

class DetailRouteTest {

    @Test
    fun repo_roundTripsThroughRoute() {
        val repo = SearchResultItem.RepoResult(
            id = "1",
            name = "kotlin",
            fullName = "JetBrains/kotlin",
            ownerLogin = "JetBrains",
            avatarUrl = null,
            description = "Quotes \" and slashes / survive",
            stars = 48000
        )

        assertEquals(repo, repo.toDetailRoute().toItem())
    }

    @Test
    fun user_roundTripsThroughRoute() {
        val user = SearchResultItem.UserResult(
            id = "2",
            login = "octocat",
            avatarUrl = "https://avatars.githubusercontent.com/u/583231?v=4",
            htmlUrl = "https://github.com/octocat"
        )

        assertEquals(user, user.toDetailRoute().toItem())
    }
}
