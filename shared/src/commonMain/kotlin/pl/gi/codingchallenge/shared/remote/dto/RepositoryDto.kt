package pl.gi.codingchallenge.shared.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RepositoryDto(
    val id: Long,
    val name: String,
    @SerialName("full_name") val fullName: String,
    val owner: UserDto,
    val description: String? = null,
    @SerialName("stargazers_count") val stargazersCount: Int
)
