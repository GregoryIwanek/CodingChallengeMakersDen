package pl.gi.codingchallenge.shared.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.koin.dsl.module

// Only job: supply the platform-specific HttpClientEngine sharedModule's CatFactApi
// binding asks for via get(). An iosPlatformModule with Darwin.create() would play
// the same role once iosMain exists - commonMain code never changes either way.
val androidPlatformModule = module {
    single<HttpClientEngine> { OkHttp.create() }
}
