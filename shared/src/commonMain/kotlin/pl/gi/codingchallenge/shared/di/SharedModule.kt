package pl.gi.codingchallenge.shared.di

import org.koin.dsl.module
import pl.gi.codingchallenge.shared.cache.SearchResultCache
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchCache
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchNetworkSource
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
    // same pattern the deleted CatFactApi proved. Not yet consumed by anything: :app
    // still uses its own Retrofit-based GitHubSearchNetworkSourceImpl until step 5
    // wires CachingGitHubSearchRepository through this one instead.
    single { GitHubApi(engine = get()) }
    single<GitHubSearchNetworkSource> { GitHubSearchNetworkSourceImpl(api = get()) }
}
