plugins {
    `kotlin-dsl`
}

group = "pl.gi.codingchallenge.buildlogic"

dependencies {
    // compileOnly: the real plugin versions come from the root build's
    // classpath (its `apply false` declarations), so convention plugins
    // compile against them without pinning a second copy.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ktlint.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = libs.plugins.codingchallenge.android.library.get().pluginId
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = libs.plugins.codingchallenge.android.compose.get().pluginId
            implementationClass = "AndroidComposeConventionPlugin"
        }
    }
}
