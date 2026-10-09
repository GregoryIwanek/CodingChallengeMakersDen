plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    application
}

// Plain JVM server: holds the Anthropic API key so it never ships in the APK.
// Run with `./gradlew :proxy:run` (needs ANTHROPIC_API_KEY in the environment).
kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("pl.gi.codingchallenge.proxy.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.anthropic.java)
    implementation(libs.kotlinx.coroutines.core)
    runtimeOnly(libs.logback.classic)

    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
