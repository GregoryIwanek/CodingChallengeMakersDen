package pl.gi.codingchallenge.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchRepository
import pl.gi.codingchallenge.shared.domain.usecase.SearchAutocompleteUseCase

/**
 * Replaces the Koin bridge in instrumented tests: the real use case
 * (debounce, min-length gate) over a repository with fixed, offline results.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [SharedKoinBridgeModule::class])
object FakeSearchModule {

    val REPO: SearchResultItem.RepoResult = SearchResultItem.RepoResult(
        id = "1",
        name = "kotlin",
        fullName = "JetBrains/kotlin",
        ownerLogin = "JetBrains",
        avatarUrl = null,
        description = "The Kotlin Programming Language",
        stars = 48000
    )

    val USER: SearchResultItem.UserResult = SearchResultItem.UserResult(
        id = "2",
        login = "kotlindev",
        avatarUrl = null,
        htmlUrl = "https://github.com/kotlindev"
    )

    @Provides
    fun provideGitHubSearchRepository(): GitHubSearchRepository = object : GitHubSearchRepository {
        override suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem> =
            listOf(REPO, USER)
    }

    @Provides
    fun provideSearchAutocompleteUseCase(
        repository: GitHubSearchRepository
    ): SearchAutocompleteUseCase = SearchAutocompleteUseCase(repository)
}
