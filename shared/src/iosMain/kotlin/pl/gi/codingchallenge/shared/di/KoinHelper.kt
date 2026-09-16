package pl.gi.codingchallenge.shared.di

import org.koin.mp.KoinPlatform
import pl.gi.codingchallenge.shared.domain.repository.GitHubSearchRepository

// Swift-callable Koin accessors live here. Swift never constructs
// CachingGitHubSearchRepository by hand - it resolves the same Koin-managed
// singleton :app gets via SharedKoinBridgeModule on Android, through the global
// Koin context startKoin { } (KoinBootstrap.kt) put in place.
//
// Deliberately not SearchAutocompleteUseCase: verified live (inspecting the
// generated Shared.h) that Kotlin/Native's default Flow export - both
// invoke(requests:)'s Flow parameter and its Flow return - is a bare
// completion-handler-based protocol with no MutableStateFlow constructor exported
// and no AsyncSequence/`for await` support. ContentView instead re-implements the
// debounce/min-length policy natively in Swift over this plain suspend fun, the
// same division of responsibility Android's ViewModel/use-case split already has.
fun getGitHubSearchRepository(): GitHubSearchRepository = KoinPlatform.getKoin().get()
