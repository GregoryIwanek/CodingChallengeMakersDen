package pl.gi.codingchallenge.data.model

import pl.gi.codingchallenge.data.remote.dto.RepositoryDto
import pl.gi.codingchallenge.data.remote.dto.UserDto
import pl.gi.codingchallenge.domain.model.SearchResultItem

fun RepositoryDto.toDomain(): SearchResultItem.RepoResult = SearchResultItem.RepoResult(
    id = id.toString(),
    name = name,
    fullName = fullName,
    ownerLogin = owner.login,
    avatarUrl = owner.avatarUrl,
    description = description,
    stars = stargazersCount,
)

fun UserDto.toDomain(): SearchResultItem.UserResult = SearchResultItem.UserResult(
    id = id.toString(),
    login = login,
    avatarUrl = avatarUrl,
    htmlUrl = htmlUrl,
)
