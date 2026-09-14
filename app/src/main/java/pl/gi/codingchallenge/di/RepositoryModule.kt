package pl.gi.codingchallenge.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.gi.codingchallenge.data.repository.CachingGitHubSearchRepository
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // GitHubSearchRepositoryImpl (Retrofit) is still a plain @Inject-constructed
    // class - CachingGitHubSearchRepository wraps it directly. This is the entire
    // payoff of coding to an interface since step 5: swapping the live
    // implementation is a one-line change here, invisible to every caller.
    @Binds
    abstract fun bindGitHubSearchRepository(
        cachingGitHubSearchRepository: CachingGitHubSearchRepository,
    ): GitHubSearchRepository
}
