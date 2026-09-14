package pl.gi.codingchallenge.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pl.gi.codingchallenge.data.model.toDomain
import pl.gi.codingchallenge.data.remote.GitHubApi
import pl.gi.codingchallenge.shared.domain.mergeAndSort
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchRepository
import javax.inject.Inject

class GitHubSearchRepositoryImpl @Inject constructor(
    private val api: GitHubApi,
) : GitHubSearchRepository {

    override suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem> = coroutineScope {
        val usersDeferred = async {
            api.searchUsers(query = query, perPage = perTypeLimit).items.map { it.toDomain() }
        }
        val reposDeferred = async {
            api.searchRepositories(query = query, perPage = perTypeLimit).items.map { it.toDomain() }
        }

        mergeAndSort(users = usersDeferred.await(), repos = reposDeferred.await())
    }
}
