package pl.gi.codingchallenge

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Runs instrumented tests under HiltTestApplication instead of
 * CodingChallengeApp, so tests can replace Hilt bindings (and Koin is never
 * started - the fake module below doesn't need it).
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
