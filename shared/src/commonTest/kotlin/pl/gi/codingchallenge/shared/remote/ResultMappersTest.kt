package pl.gi.codingchallenge.shared.remote

import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.remote.dto.RepositoryDto
import pl.gi.codingchallenge.shared.remote.dto.UserDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResultMappersTest {

    @Test
    fun repositoryDtoToDomainMapsEveryFieldConvertingIdToAString() {
        val dto = RepositoryDto(
            id = 123L,
            name = "kotlin",
            fullName = "JetBrains/kotlin",
            owner = UserDto(
                id = 456L,
                login = "JetBrains",
                avatarUrl = "https://avatars/jetbrains.png",
                htmlUrl = "https://github.com/JetBrains",
            ),
            description = "The Kotlin Programming Language",
            stargazersCount = 48000,
        )

        val result = dto.toDomain()

        assertEquals(
            SearchResultItem.RepoResult(
                id = "123",
                name = "kotlin",
                fullName = "JetBrains/kotlin",
                ownerLogin = "JetBrains",
                avatarUrl = "https://avatars/jetbrains.png",
                description = "The Kotlin Programming Language",
                stars = 48000,
            ),
            result,
        )
    }

    @Test
    fun repositoryDtoToDomainPreservesANullDescription() {
        val dto = RepositoryDto(
            id = 1L,
            name = "repo",
            fullName = "owner/repo",
            owner = UserDto(id = 2L, login = "owner", htmlUrl = "https://github.com/owner"),
            description = null,
            stargazersCount = 0,
        )

        assertNull(dto.toDomain().description)
    }

    @Test
    fun userDtoToDomainMapsEveryFieldConvertingIdToAString() {
        val dto = UserDto(
            id = 789L,
            login = "octocat",
            avatarUrl = "https://avatars/octocat.png",
            htmlUrl = "https://github.com/octocat",
        )

        val result = dto.toDomain()

        assertEquals(
            SearchResultItem.UserResult(
                id = "789",
                login = "octocat",
                avatarUrl = "https://avatars/octocat.png",
                htmlUrl = "https://github.com/octocat",
            ),
            result,
        )
    }

    @Test
    fun userDtoToDomainPreservesANullAvatarUrl() {
        val dto = UserDto(id = 1L, login = "octocat", avatarUrl = null, htmlUrl = "https://github.com/octocat")

        assertNull(dto.toDomain().avatarUrl)
    }
}
