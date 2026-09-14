package pl.gi.codingchallenge.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.gi.codingchallenge.data.remote.GitHubApi
import pl.gi.codingchallenge.data.remote.dto.GitHubSearchResponse
import pl.gi.codingchallenge.data.remote.dto.RepositoryDto
import pl.gi.codingchallenge.data.remote.dto.UserDto
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem

class GitHubSearchRepositoryImplTest {

    private val api = mockk<GitHubApi>()
    private val repository = GitHubSearchRepositoryImpl(api)

    @Test
    fun `search calls both endpoints with the given query and perPage`() = runTest {
        coEvery { api.searchUsers(query = "kot", perPage = 10) } returns emptyUserResponse()
        coEvery { api.searchRepositories(query = "kot", perPage = 10) } returns emptyRepoResponse()

        repository.search(query = "kot", perTypeLimit = 10)

        coVerify(exactly = 1) { api.searchUsers(query = "kot", perPage = 10) }
        coVerify(exactly = 1) { api.searchRepositories(query = "kot", perPage = 10) }
    }

    @Test
    fun `search maps and merges both response types into sorted domain results`() = runTest {
        coEvery { api.searchUsers(query = "kot", perPage = 50) } returns GitHubSearchResponse(
            totalCount = 1,
            incompleteResults = false,
            items = listOf(
                UserDto(
                    id = 1, login = "zzz-user", avatarUrl = null,
                    htmlUrl = "https://github.com/zzz-user",
                ),
            ),
        )
        coEvery { api.searchRepositories(query = "kot", perPage = 50) } returns GitHubSearchResponse(
            totalCount = 1,
            incompleteResults = false,
            items = listOf(
                RepositoryDto(
                    id = 2, name = "aaa-repo", fullName = "owner/aaa-repo",
                    owner = UserDto(
                        id = 3, login = "owner", avatarUrl = null,
                        htmlUrl = "https://github.com/owner",
                    ),
                    description = "A repo", stargazersCount = 42,
                ),
            ),
        )

        val results = repository.search(query = "kot")

        assertEquals(2, results.size)
        assertEquals("aaa-repo", (results[0] as SearchResultItem.RepoResult).name)
        assertEquals("zzz-user", (results[1] as SearchResultItem.UserResult).login)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `search fetches users and repositories in parallel, not sequentially`() = runTest {
        coEvery { api.searchUsers(query = "kot", perPage = 50) } coAnswers {
            delay(1000)
            emptyUserResponse()
        }
        coEvery { api.searchRepositories(query = "kot", perPage = 50) } coAnswers {
            delay(1000)
            emptyRepoResponse()
        }

        repository.search(query = "kot")

        // Sequential calls would take ~2000ms of virtual time; parallel async{} takes ~1000ms.
        assertTrue("expected ~1000ms elapsed, was ${currentTime}ms", currentTime < 2000)
    }

    private fun emptyUserResponse() = GitHubSearchResponse<UserDto>(
        totalCount = 0, incompleteResults = false, items = emptyList(),
    )

    private fun emptyRepoResponse() = GitHubSearchResponse<RepositoryDto>(
        totalCount = 0, incompleteResults = false, items = emptyList(),
    )
}
