package pl.gi.codingchallenge.shared.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module
import pl.gi.codingchallenge.shared.cache.CacheDatabase

// Supplies the platform-specific pieces sharedModule's bindings ask for via get():
// the Ktor engine, and now the SQLDelight driver. An iosPlatformModule would supply
// Darwin.create() and a NativeSqliteDriver instead once iosMain exists - commonMain
// code never changes either way.
val androidPlatformModule = module {
    single<HttpClientEngine> { OkHttp.create() }
    // get() resolves the Context koin-android's androidContext() registered in
    // CodingChallengeApp's startKoin {} call - no separate Context-provisioning
    // mechanism needed. Passing CacheDatabase.Schema handles create/upgrade
    // automatically on Android (contrast with the JVM test driver in commonTest).
    single<SqlDriver> { AndroidSqliteDriver(CacheDatabase.Schema, get(), "search_cache.db") }
}
