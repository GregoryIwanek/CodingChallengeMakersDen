package pl.gi.codingchallenge.shared.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import pl.gi.codingchallenge.shared.domain.mergeAndSort
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchNetworkSource

class GitHubSearchNetworkSourceImpl(
    private val api: GitHubApi,
) : GitHubSearchNetworkSource {

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
