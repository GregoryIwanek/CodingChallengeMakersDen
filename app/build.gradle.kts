plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.codingchallenge.android.hilt)
    alias(libs.plugins.ktlint)
}

ktlint {
    // Android Kotlin Style Guide compatibility (e.g. import-order handling)
    // rather than the plain Kotlin style guide.
    android.set(true)
}

android {
    namespace = "pl.gi.codingchallenge"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pl.gi.codingchallenge"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Swaps in HiltTestApplication so @HiltAndroidTest tests can replace
        // bindings; plain createComposeRule tests run under it unchanged.
        testInstrumentationRunner = "pl.gi.codingchallenge.HiltTestRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(project(":feature:detail"))
    implementation(project(":feature:autocomplete"))

    implementation(libs.androidx.core.ktx)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // DI
    implementation(libs.hilt.navigation.compose)
    // Hilt stays the DI system for Android-only classes; Koin owns :shared's own
    // graph, bridged into Hilt by a small module (see di/SharedKoinBridgeModule).
    implementation(libs.koin.android)

    // Networking - Retrofit/OkHttp are gone; GitHub-search networking moved into
    // :shared onto Ktor. ktor-client-okhttp remains: it's the real HttpClientEngine
    // :shared's GitHubApi resolves via Koin on Android (see androidPlatformModule).
    implementation(libs.ktor.client.okhttp)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
