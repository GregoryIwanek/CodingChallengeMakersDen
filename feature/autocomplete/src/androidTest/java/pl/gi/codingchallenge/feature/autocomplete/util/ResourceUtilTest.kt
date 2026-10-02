package pl.gi.codingchallenge.feature.autocomplete.util

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import pl.gi.codingchallenge.feature.autocomplete.R

/** Isolates spRes()'s density and fontScale divisions via LocalDensity overrides. */
class ResourceUtilTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    @Test
    fun spRes_dividesOutOverriddenDensity() {
        var result: Float? = null

        composeRule.setContent {
            val real: Density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides
                    Density(density = real.density * 2f, fontScale = real.fontScale)
            ) {
                result = spRes(R.dimen.autocomplete_input_text_size).value
            }
        }

        assertEquals(7.5f, result!!, 0.01f) // 15sp halved by the doubled density
    }

    @Test
    fun spRes_dividesOutOverriddenFontScale() {
        var result: Float? = null

        composeRule.setContent {
            val real: Density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides
                    Density(density = real.density, fontScale = real.fontScale * 2f)
            ) {
                result = spRes(R.dimen.autocomplete_input_text_size).value
            }
        }

        assertEquals(7.5f, result!!, 0.01f) // 15sp halved by the doubled fontScale
    }
}
