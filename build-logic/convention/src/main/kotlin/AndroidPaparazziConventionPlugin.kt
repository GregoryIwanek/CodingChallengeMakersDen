import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

/**
 * Paparazzi golden-image tests for a library module, with `check` verifying
 * the goldens so a plain `./gradlew check` catches visual regressions.
 */
class AndroidPaparazziConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.cash.paparazzi")

            tasks.withType<Test>().configureEach {
                // Paparazzi + Gradle 9 has a known HTML-report generation
                // issue - see https://github.com/cashapp/paparazzi/issues/2111
                reports.html.required.set(false)
            }

            tasks.named("check") {
                dependsOn("verifyPaparazziDebug")
            }
        }
    }
}
