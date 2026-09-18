package pl.gi.codingchallenge.shared.remote

import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.remote.dto.RepositoryDto
import pl.gi.codingchallenge.shared.remote.dto.UserDto

fun RepositoryDto.toDomain(): SearchResultItem.RepoResult = SearchResultItem.RepoResult(
    id = id.toString(),
    name = name,
    fullName = fullName,
    ownerLogin = owner.login,
    avatarUrl = owner.avatarUrl,
    description = description,
    stars = stargazersCount
)

fun UserDto.toDomain(): SearchResultItem.UserResult = SearchResultItem.UserResult(
    id = id.toString(),
    login = login,
    avatarUrl = avatarUrl,
    htmlUrl = htmlUrl
)
