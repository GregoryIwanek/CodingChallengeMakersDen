package pl.gi.codingchallenge.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import pl.gi.codingchallenge.shared.cache.SearchResultCache
import pl.gi.codingchallenge.shared.catfact.CatFactApi
import pl.gi.codingchallenge.shared.history.ConversionHistoryRepository

// The entire Hilt/Koin bridge: Hilt generates its graph at compile time via
// annotation processing and has no way to reach into Koin's runtime-built graph on
// its own. A Hilt @Module is just a plain Kotlin object, so it can implement
// KoinComponent like any other class - get() below is KoinComponent's extension
// function, resolving against whatever Koin instance startKoin {} registered.
@Module
@InstallIn(SingletonComponent::class)
object SharedKoinBridgeModule : KoinComponent {

    @Provides
    fun provideConversionHistoryRepository(): ConversionHistoryRepository = get()

    @Provides
    fun provideCatFactApi(): CatFactApi = get()

    @Provides
    fun provideSearchResultCache(): SearchResultCache = get()
}
