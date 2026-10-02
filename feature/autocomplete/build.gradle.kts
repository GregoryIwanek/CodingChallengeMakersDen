plugins {
    alias(libs.plugins.codingchallenge.android.library)
    alias(libs.plugins.codingchallenge.android.compose)
    alias(libs.plugins.codingchallenge.android.paparazzi)
    alias(libs.plugins.codingchallenge.android.hilt)
}

android {
    namespace = "pl.gi.codingchallenge.feature.autocomplete"
}

dependencies {
    implementation(project(":shared"))
}
