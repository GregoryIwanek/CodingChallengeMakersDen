package pl.gi.codingchallenge.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.gi.codingchallenge.data.repository.CachingGitHubSearchRepository
import pl.gi.codingchallenge.data.repository.GitHubSearchNetworkSourceImpl
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchNetworkSource
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // CachingGitHubSearchRepository is the one thing the domain layer (the use
    // case) ever resolves as GitHubSearchRepository. This is the entire payoff of
    // coding to an interface since step 5: swapping the live implementation is a
    // one-line change here, invisible to every caller.
    @Binds
    abstract fun bindGitHubSearchRepository(
        cachingGitHubSearchRepository: CachingGitHubSearchRepository,
    ): GitHubSearchRepository

    // GitHubSearchNetworkSourceImpl (Retrofit) fulfills only the narrower "raw
    // network access" contract - it no longer claims to be a full
    // GitHubSearchRepository itself.
    @Binds
    abstract fun bindGitHubSearchNetworkSource(
        gitHubSearchNetworkSourceImpl: GitHubSearchNetworkSourceImpl,
    ): GitHubSearchNetworkSource
}
