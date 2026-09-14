package pl.gi.codingchallenge

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pl.gi.codingchallenge.shared.di.androidPlatformModule
import pl.gi.codingchallenge.shared.di.sharedModule

@HiltAndroidApp
class CodingChallengeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Independent of Hilt - Hilt's graph is generated at compile time via
        // annotation processing, Koin's is built here at runtime. They don't know
        // about each other; SharedKoinBridgeModule is what connects them.
        startKoin {
            androidContext(this@CodingChallengeApp)
            modules(sharedModule, androidPlatformModule)
        }
    }
}
