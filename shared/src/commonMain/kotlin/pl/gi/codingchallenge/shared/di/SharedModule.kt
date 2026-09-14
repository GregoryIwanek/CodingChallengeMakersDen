package pl.gi.codingchallenge.shared.di

import org.koin.dsl.module
import pl.gi.codingchallenge.shared.cache.SearchResultCache
import pl.gi.codingchallenge.shared.catfact.CatFactApi
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchCache
import pl.gi.codingchallenge.shared.history.ConversionHistoryRepository
import pl.gi.codingchallenge.shared.history.InMemoryConversionHistoryRepository

val sharedModule = module {
    single<ConversionHistoryRepository> { InMemoryConversionHistoryRepository() }
    // get() resolves whatever HttpClientEngine binding a platform module registers
    // (androidPlatformModule today, an iosPlatformModule once iosMain exists) -
    // this module never needs to know or import anything platform-specific.
    single { CatFactApi(engine = get()) }
    // Same shape as CatFactApi: get() resolves whatever SqlDriver a platform module
    // registers. Registered under the interface type (not the concrete
    // SearchResultCache) so callers - here, SharedKoinBridgeModule - resolve and
    // depend on the abstraction, matching CachingGitHubSearchRepository's own
    // GitHubSearchCache-typed constructor parameter.
    single<GitHubSearchCache> { SearchResultCache(driver = get()) }
}
