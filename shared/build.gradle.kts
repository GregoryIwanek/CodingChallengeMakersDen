plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    // Required for @Serializable - the kotlinx-serialization-json runtime dependency
    // alone isn't enough; this compiler plugin generates the serializers.
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ktlint)
}

ktlint {
    // Android Kotlin Style Guide compatibility (e.g. import-order handling)
    // rather than the plain Kotlin style guide.
    android.set(true)
    filter {
        // SQLDelight registers its generated sources as a commonMain source
        // dir, so ktlint would otherwise lint/format generated code that's
        // regenerated fresh on every clean checkout (confirmed via a CI
        // failure - masked locally by stale build/ output from prior runs).
        exclude { element -> element.file.path.contains("/generated/") }
    }
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
    // Confirmed while adding iOS targets: this plugin combination doesn't
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

    // iOS targets - compile :shared for iosArm64/iosSimulatorArm64 via this
    // project's existing android.kotlin.multiplatform.library plugin combination.
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
            // has to be declared per-target. iosMain's source set gets ktor-client-darwin
            // instead; commonMain's code calling HttpClient(...) never changes.
            // Misplacing this in commonMain instead of here would fail the same way
            // MockK does, once a genuine non-JVM target exists.
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
        iosMain.dependencies {
            // Darwin - Ktor's engine backed by NSURLSession, the iOS counterpart to
            // androidMain's OkHttp above. Same seam: commonMain's HttpClient(...)
            // calls never change.
            implementation(libs.ktor.client.darwin)

            // NativeSqliteDriver - the iOS counterpart to AndroidSqliteDriver above.
            // Unlike Android, there's no Context to pass; only the schema and a
            // database name are needed.
            implementation(libs.sqldelight.native.driver)
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
            // on every target by construction, unlike MockK.
            implementation(libs.ktor.client.mock)
            // Turbine is genuinely multiplatform (unlike MockK) - used to assert on
            // Flow emissions in SearchAutocompleteUseCaseTest.
            implementation(libs.turbine)
        }
        // JdbcSqliteDriver moved here from commonTest: JDBC is JVM-only, so it can't
        // resolve for iosArm64/iosSimulatorArm64 test compilation once those targets
        // genuinely exist - confirmed via a real dependency-resolution failure (not a
        // compiler error) the moment iOS targets were added, same category of
        // constraint MockK hits.
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}
