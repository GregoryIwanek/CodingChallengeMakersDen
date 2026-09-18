package pl.gi.codingchallenge.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.koin.core.module.Module
import org.koin.dsl.module
import pl.gi.codingchallenge.shared.cache.CacheDatabase

// iOS counterpart to androidPlatformModule - same bindings sharedModule.kt asks
// for via get(), different platform implementations behind them. Unlike
// AndroidSqliteDriver, NativeSqliteDriver has no Context to pass.
val iosPlatformModule: Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single<SqlDriver> { NativeSqliteDriver(CacheDatabase.Schema, "search_cache.db") }
}
