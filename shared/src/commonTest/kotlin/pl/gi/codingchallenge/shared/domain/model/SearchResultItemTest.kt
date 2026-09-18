package pl.gi.codingchallenge.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertNotEquals

class SearchResultItemTest {

    // The real bug this locks in: RepoResult.id and UserResult.id come from GitHub's own
    // independent id sequences (repo ids, user ids) - a user and a repo can legitimately
    // share the same numeric id. List/LazyColumn keys must use uniqueKey, not id, or a
    // merged list containing such a pair crashes Compose's LazyColumn ("Key ... was already
    // used") - see docs/backlog.md AT-1.
    @Test
    fun repoAndUserSharingTheSameIdStillGetDistinctUniqueKeys() {
        val repo: SearchResultItem.RepoResult = SearchResultItem.RepoResult(
            id = "1",
            name = "kotlin",
            fullName = "JetBrains/kotlin",
            ownerLogin = "JetBrains",
            avatarUrl = null,
            description = null,
            stars = 0
        )
        val user: SearchResultItem.UserResult = SearchResultItem.UserResult(
            id = "1",
            login = "octocat",
            avatarUrl = null,
            htmlUrl = "https://github.com/octocat"
        )

        assertNotEquals(repo.uniqueKey, user.uniqueKey)
    }
}
