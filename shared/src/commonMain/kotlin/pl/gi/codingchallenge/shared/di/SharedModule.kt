package pl.gi.codingchallenge.shared.di

import org.koin.dsl.module
import pl.gi.codingchallenge.shared.cache.SearchResultCache
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchCache

val sharedModule = module {
    // get() resolves whatever SqlDriver a platform module registers
    // (androidPlatformModule today, an iosPlatformModule once iosMain exists).
    // Registered under the interface type (not the concrete SearchResultCache) so
    // callers - here, SharedKoinBridgeModule - resolve and depend on the
    // abstraction, matching CachingGitHubSearchRepository's own
    // GitHubSearchCache-typed constructor parameter.
    single<GitHubSearchCache> { SearchResultCache(driver = get()) }
}
