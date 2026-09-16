package pl.gi.codingchallenge.shared.di

import org.koin.core.context.startKoin

// There's no Application.onCreate() equivalent on iOS for Koin to hook into
// automatically (contrast with CodingChallengeApp's startKoin {} call on
// Android) - Swift has to call this explicitly, once, at app launch.
fun initKoin() {
    startKoin {
        modules(sharedModule, iosPlatformModule)
    }
}
