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
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    // Parent of the debug-only Paparazzi theme (src/debug/res/values/themes.xml).
    debugImplementation(libs.material)
}
