package pl.gi.codingchallenge.shared.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import pl.gi.codingchallenge.shared.domain.mergeAndSort
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchNetworkSource

class GitHubSearchNetworkSourceImpl(private val api: GitHubApi) : GitHubSearchNetworkSource {

    // supervisorScope, not coroutineScope: with a plain coroutineScope, one branch throwing
    // cancels the sibling too (structured concurrency's normal fail-fast behavior) - discarding
    // real, already-fetched results if only one of users/repos actually failed (e.g. the repos
    // search 4xx's after the users search already succeeded). See docs/backlog.md AT-12.
    override suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem> =
        supervisorScope {
            val usersDeferred = async {
                catchingCancellationAware {
                    api.searchUsers(query = query, perPage = perTypeLimit).items.map {
                        it.toDomain()
                    }
                }
            }
            val reposDeferred = async {
                catchingCancellationAware {
                    api.searchRepositories(query = query, perPage = perTypeLimit).items.map {
                        it.toDomain()
                    }
                }
            }

            val users = usersDeferred.await()
            val repos = reposDeferred.await()

            if (users.isFailure && repos.isFailure) {
                // Nothing to return - propagate a real error instead of a generic "search
                // failed." Picking repos' exception is an arbitrary but deterministic choice;
                // both branches hit the same GitHubApi, so either is equally representative.
                throw repos.exceptionOrNull()!!
            }

            mergeAndSort(
                users = users.getOrDefault(emptyList()),
                repos = repos.getOrDefault(emptyList())
            )
        }
}

// Same shape as kotlin.runCatching, except it doesn't catch CancellationException - matches
// the rule CachingGitHubSearchRepository's catch block needed fixing for (docs/backlog.md
// AT-2). Verified empirically, not just by following the rule: for this exact shape (awaiting
// both Deferreds right after launching them), a plain runCatching turns out to be
// behaviorally identical here - a cancelled Deferred's .await() still throws
// CancellationException regardless of what its own coroutine body caught and returned,
// because Job cancellation state is tracked independently of the returned value. This
// function is kept anyway because that equivalence is an implementation detail of this
// specific await-both shape, not a guarantee - e.g. it would stop holding if this ever
// changed to not await() every branch unconditionally. Follow the rule; don't rely on why it
// happens not to matter today.
private suspend fun <T> catchingCancellationAware(block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Result.failure(e)
}
