plugins {
    alias(libs.plugins.codingchallenge.android.library)
    alias(libs.plugins.codingchallenge.android.compose)
    alias(libs.plugins.codingchallenge.android.paparazzi)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "pl.gi.codingchallenge.feature.detail"
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
