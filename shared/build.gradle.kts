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

            // Ktor networking spike (kmp-interview-prep step 3), pre-flight only so
            // far. HttpClient itself is fully multiplatform Kotlin - the engine
            // (step 2) is the only genuinely per-target piece. No BOM here - see the
            // version.ref comment in libs.versions.toml for why.
            implementation(libs.ktor.client.core)
        }
        commonTest.dependencies {
            // kotlin("test") is the multiplatform test-annotations artifact: the same
            // @Test/assertEquals calls resolve to a different real implementation per
            // target (JUnit on Android here, XCTest-backed on iOS later) instead of
            // pulling in one JVM-specific framework like JUnit4 or MockK directly.
            implementation(kotlin("test"))
            // Gives commonTest access to runTest {} for testing suspend functions/Flow.
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
