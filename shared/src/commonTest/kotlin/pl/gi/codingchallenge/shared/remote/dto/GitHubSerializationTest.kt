package pl.gi.codingchallenge.shared.remote.dto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Decodes real (trimmed) GitHub payloads to catch a wrong @SerialName. */
class GitHubSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun decodesARepositorySearchResponse() {
        val response = json.decodeFromString<GitHubSearchResponse<RepositoryDto>>(
            """
            {
              "total_count": 1,
              "incomplete_results": false,
              "items": [
                {
                  "id": 23096959,
                  "name": "kotlin",
                  "full_name": "JetBrains/kotlin",
                  "private": false,
                  "owner": {
                    "login": "JetBrains",
                    "id": 878437,
                    "avatar_url": "https://avatars.githubusercontent.com/u/878437?v=4",
                    "html_url": "https://github.com/JetBrains"
                  },
                  "description": "The Kotlin Programming Language.",
                  "stargazers_count": 48000,
                  "language": "Kotlin"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, response.totalCount)
        assertEquals(false, response.incompleteResults)

        val repo = response.items.single()
        assertEquals(23096959L, repo.id)
        assertEquals("kotlin", repo.name)
        assertEquals("JetBrains/kotlin", repo.fullName)
        assertEquals("The Kotlin Programming Language.", repo.description)
        assertEquals(48000, repo.stargazersCount)
        assertEquals("JetBrains", repo.owner.login)
        assertEquals("https://avatars.githubusercontent.com/u/878437?v=4", repo.owner.avatarUrl)
        assertEquals("https://github.com/JetBrains", repo.owner.htmlUrl)
    }

    @Test
    fun decodesAUserSearchResponseWithANullAvatarUrlWhenAbsent() {
        val response = json.decodeFromString<GitHubSearchResponse<UserDto>>(
            """
            {
              "total_count": 1,
              "incomplete_results": false,
              "items": [
                {
                  "login": "octocat",
                  "id": 583231,
                  "html_url": "https://github.com/octocat",
                  "type": "User",
                  "score": 1.0
                }
              ]
            }
            """.trimIndent()
        )

        val user = response.items.single()
        assertEquals(583231L, user.id)
        assertEquals("octocat", user.login)
        assertEquals("https://github.com/octocat", user.htmlUrl)
        assertNull(user.avatarUrl) // omitted from the payload entirely
    }
}
