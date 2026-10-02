package pl.gi.codingchallenge.feature.detail

import org.junit.Assert.assertEquals
import org.junit.Test
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

class GitHubUrlTest {

    @Test
    fun repo_isBuiltFromFullName() {
        val repo = SearchResultItem.RepoResult(
            id = "1",
            name = "kotlin",
            fullName = "JetBrains/kotlin",
            ownerLogin = "JetBrains",
            avatarUrl = null,
            description = null,
            stars = 0
        )

        assertEquals("https://github.com/JetBrains/kotlin", repo.gitHubUrl())
    }

    @Test
    fun user_usesHtmlUrl() {
        val user = SearchResultItem.UserResult(
            id = "2",
            login = "octocat",
            avatarUrl = null,
            htmlUrl = "https://github.com/octocat"
        )

        assertEquals("https://github.com/octocat", user.gitHubUrl())
    }
}
