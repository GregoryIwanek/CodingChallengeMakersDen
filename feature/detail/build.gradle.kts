plugins {
    alias(libs.plugins.codingchallenge.android.library)
    alias(libs.plugins.codingchallenge.android.compose)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "pl.gi.codingchallenge.feature.detail"
}

dependencies {
    implementation(project(":shared"))

    testImplementation(libs.junit)
}

tasks.withType<Test>().configureEach {
    // Same Paparazzi + Gradle 9 HTML-report workaround as :app - see
    // https://github.com/cashapp/paparazzi/issues/2111
    reports.html.required = false
}

tasks.named("check") {
    dependsOn("verifyPaparazziDebug")
}
