package pl.gi.codingchallenge.shared.di

import org.koin.mp.KoinPlatform
import pl.gi.codingchallenge.shared.catfact.CatFactApi

// Swift never constructs CatFactApi (or a Darwin engine) by hand - it resolves the
// same Koin-managed singleton :app gets via SharedKoinBridgeModule on Android,
// through the global Koin context startKoin { } (KoinBootstrap.kt) put in place.
fun getCatFactApi(): CatFactApi = KoinPlatform.getKoin().get()
