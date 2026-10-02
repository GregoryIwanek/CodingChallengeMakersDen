plugins {
    alias(libs.plugins.codingchallenge.android.library)
    alias(libs.plugins.codingchallenge.android.compose)
    alias(libs.plugins.paparazzi)
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

tasks.withType<Test>().configureEach {
    // Same Paparazzi + Gradle 9 HTML-report workaround as :app - see
    // https://github.com/cashapp/paparazzi/issues/2111
    reports.html.required = false
}

tasks.named("check") {
    dependsOn("verifyPaparazziDebug")
}
