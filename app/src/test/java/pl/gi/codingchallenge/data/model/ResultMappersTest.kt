package pl.gi.codingchallenge.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pl.gi.codingchallenge.data.remote.dto.RepositoryDto
import pl.gi.codingchallenge.data.remote.dto.UserDto
import pl.gi.codingchallenge.domain.model.SearchResultItem

class ResultMappersTest {

    @Test
    fun `RepositoryDto toDomain maps every field, converting id to a String`() {
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
    fun `RepositoryDto toDomain preserves a null description`() {
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
    fun `UserDto toDomain maps every field, converting id to a String`() {
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
    fun `UserDto toDomain preserves a null avatarUrl`() {
        val dto = UserDto(id = 1L, login = "octocat", avatarUrl = null, htmlUrl = "https://github.com/octocat")

        assertNull(dto.toDomain().avatarUrl)
    }
}
