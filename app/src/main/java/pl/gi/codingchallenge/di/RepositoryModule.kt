package pl.gi.codingchallenge.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.gi.codingchallenge.data.repository.GitHubSearchRepositoryImpl
import pl.gi.codingchallenge.domain.repository.GitHubSearchRepository

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindGitHubSearchRepository(
        gitHubSearchRepository: GitHubSearchRepositoryImpl,
    ): GitHubSearchRepository
}
