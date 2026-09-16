package pl.gi.codingchallenge.shared.di

import org.koin.dsl.module
import pl.gi.codingchallenge.shared.cache.SearchResultCache
import pl.gi.codingchallenge.shared.domain.repository.CachingGitHubSearchRepository
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchCache
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchNetworkSource
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchRepository
import pl.gi.codingchallenge.shared.domain.usecase.SearchAutocompleteUseCase
import pl.gi.codingchallenge.shared.remote.GitHubApi
import pl.gi.codingchallenge.shared.remote.GitHubSearchNetworkSourceImpl

val sharedModule = module {
    // get() resolves whatever SqlDriver a platform module registers
    // (androidPlatformModule today, an iosPlatformModule once iosMain exists).
    // Registered under the interface type (not the concrete SearchResultCache) so
    // callers - here, SharedKoinBridgeModule - resolve and depend on the
    // abstraction, matching CachingGitHubSearchRepository's own
    // GitHubSearchCache-typed constructor parameter.
    single<GitHubSearchCache> { SearchResultCache(driver = get()) }
    // get() resolves whatever HttpClientEngine binding a platform module registers -
    // same pattern the deleted CatFactApi proved.
    single { GitHubApi(engine = get()) }
    single<GitHubSearchNetworkSource> { GitHubSearchNetworkSourceImpl(api = get()) }
    // CachingGitHubSearchRepository is the one thing the domain layer (the use case)
    // ever resolves as GitHubSearchRepository - this is now the single live
    // implementation for both :app (via SharedKoinBridgeModule) and iOS.
    single<GitHubSearchRepository> { CachingGitHubSearchRepository(network = get(), cache = get()) }
    single { SearchAutocompleteUseCase(repository = get()) }
}
