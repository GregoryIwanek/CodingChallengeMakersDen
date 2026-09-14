plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "pl.gi.codingchallenge.shared"
        compileSdk = 37
        minSdk = 26

        withHostTestBuilder {}
    }
    // iosMain targets intentionally NOT added yet — that's step 8.

    // Kotlin source sets = per-target source folders. commonMain/commonTest here compile
    // against every target this module declares (just androidMain for now); androidMain
    // would get its own dependencies block if it needed Android-only libraries.
    sourceSets {
        commonMain.dependencies {
            // The multiplatform coroutines artifact (-core), not the Android-only
            // -android one :app uses — this has to compile for every target, so it
            // can't depend on anything Android-specific.
            implementation(libs.kotlinx.coroutines.core)

            // Ktor networking spike (kmp-interview-prep step 3). HttpClient itself,
            // ContentNegotiation, and the kotlinx.serialization converter are all
            // genuinely multiplatform Kotlin - no per-target code needed. The engine
            // is the only genuinely per-target piece (see androidMain below). No BOM
            // here - see the version.ref comment in libs.versions.toml for why.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
        androidMain.dependencies {
            // The HTTP engine is a thin wrapper around a platform-specific networking
            // stack (OkHttp here) - unlike everything else Ktor-related above, this
            // has to be declared per-target. When iosMain arrives (step 8), that
            // source set gets ktor-client-darwin instead; commonMain's code calling
            // HttpClient(...) never changes. Misplacing this in commonMain instead of
            // here would fail the same way step 2's MockK exercise did, once a
            // genuine non-JVM target exists.
            implementation(libs.ktor.client.okhttp)
        }
        commonTest.dependencies {
            // kotlin("test") is the multiplatform test-annotations artifact: the same
            // @Test/assertEquals calls resolve to a different real implementation per
            // target (JUnit on Android here, XCTest-backed on iOS later) instead of
            // pulling in one JVM-specific framework like JUnit4 or MockK directly.
            implementation(kotlin("test"))
            // Gives commonTest access to runTest {} for testing suspend functions/Flow.
            implementation(libs.kotlinx.coroutines.test)
            // Ktor's fake HTTP engine for tests - a real, minimal HttpClientEngine
            // implementation, not a mocking framework like MockK. Compiles and works
            // on every target by construction (step 2's "fakes, not mocks" lesson).
            implementation(libs.ktor.client.mock)
        }
    }
}
