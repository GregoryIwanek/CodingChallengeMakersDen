plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    // Required for @Serializable - the kotlinx-serialization-json runtime dependency
    // alone isn't enough; this compiler plugin generates the serializers.
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

// Persistence cache, stretch goal - a small cache in front of the real
// GitHubSearchRepository, network-first with cache-as-fallback-on-failure.
sqldelight {
    databases {
        register("CacheDatabase") {
            packageName.set("pl.gi.codingchallenge.shared.cache")
        }
    }
}

kotlin {
    // Confirmed while adding iOS targets (step 8): this plugin combination doesn't
    // auto-create the iosMain/iosTest intermediate source sets the way a plain
    // org.jetbrains.kotlin.multiplatform module does - without this call, only the
    // per-target iosArm64Main/iosSimulatorArm64Main source sets exist, with no
    // shared place to put code common to both iOS targets.
    applyDefaultHierarchyTemplate()

    android {
        namespace = "pl.gi.codingchallenge.shared"
        compileSdk = 37
        minSdk = 26

        withHostTestBuilder {}
    }

    // Step 8: iOS targets. Empty skeleton for now — no iosMain code yet, this pass
    // only proves the Gradle/Kotlin-Native toolchain resolves for this project's
    // existing android.kotlin.multiplatform.library plugin combination.
    iosArm64()
    iosSimulatorArm64()

    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    // Kotlin source sets = per-target source folders. commonMain/commonTest here compile
    // against every target this module declares; androidMain/iosMain get their own
    // dependencies blocks for platform-only libraries.
    sourceSets {
        commonMain.dependencies {
            // The multiplatform coroutines artifact (-core), not the Android-only
            // -android one :app uses — this has to compile for every target, so it
            // can't depend on anything Android-specific.
            implementation(libs.kotlinx.coroutines.core)

            // Ktor networking spike. HttpClient itself, ContentNegotiation, and the
            // kotlinx.serialization converter are all genuinely multiplatform Kotlin -
            // no per-target code needed. The engine is the only genuinely per-target
            // piece (see androidMain below). No BOM here - see the version.ref comment
            // in libs.versions.toml for why.
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Koin DI. No BOM - see the version.ref comment in libs.versions.toml for why.
            implementation(libs.koin.core)

            // SQLDelight cache. Runtime + coroutines-extensions are genuinely
            // multiplatform Kotlin - the driver (see androidMain below) is the one
            // per-target piece, same shape as Ktor's engine split above.
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines.extensions)

            // SearchResultCache calls Json.encodeToString/decodeFromString directly,
            // not just transitively via ktor-serialization-kotlinx-json.
            implementation(libs.kotlinx.serialization.json)
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

            // koin-android adds Android-specific conveniences (androidContext(),
            // lifecycle-aware scoping) on top of koin-core. This toy module doesn't
            // need Android Context for anything yet, but this is how a real
            // Android-facing KMP module's DI setup normally looks.
            implementation(libs.koin.android)

            // AndroidSqliteDriver - the platform-specific piece SQLDelight's runtime
            // needs to actually talk to real SQLite on Android.
            implementation(libs.sqldelight.android.driver)
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
        // JdbcSqliteDriver moved here from commonTest (step 8): JDBC is JVM-only,
        // so it can't resolve for iosArm64/iosSimulatorArm64 test compilation once
        // those targets genuinely exist - confirmed via a real dependency-resolution
        // failure (not a compiler error) the moment iOS targets were added, same
        // category of constraint MockK hit in step 2.
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}
