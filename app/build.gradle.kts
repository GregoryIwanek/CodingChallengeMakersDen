plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.paparazzi)
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

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // DI
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    // Hilt stays the DI system for Android-only classes; Koin owns :shared's own
    // graph, bridged into Hilt by a small module (see di/SharedKoinBridgeModule).
    implementation(libs.koin.android)

    // Networking - Retrofit/OkHttp are gone; GitHub-search networking moved into
    // :shared onto Ktor. ktor-client-okhttp remains: it's the real HttpClientEngine
    // :shared's GitHubApi resolves via Koin on Android (see androidPlatformModule).
    implementation(libs.ktor.client.okhttp)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

tasks.withType<Test>().configureEach {
    // Paparazzi + Gradle 9 (required by this project's AGP 9.3.2) has a
    // known HTML-report generation issue — see
    // https://github.com/cashapp/paparazzi/issues/2111
    reports.html.required = false
}

tasks.named("check") {
    // So a plain `./gradlew check` catches golden-image regressions too,
    // not just behavioral test failures.
    dependsOn("verifyPaparazziDebug")
}
