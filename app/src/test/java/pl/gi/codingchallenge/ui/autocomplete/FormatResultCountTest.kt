package pl.gi.codingchallenge.ui.autocomplete

import android.content.res.Resources
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi is used only for its Android [Resources] on the JVM, so the
 * plurals and capped-string lookups can be tested without Robolectric or a device.
 */
class FormatResultCountTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "en")
    )

    private val resources: Resources
        get() = paparazzi.context.resources

    @Test
    fun one_isSingular() {
        assertEquals(
            "1 result",
            formatResultCount(resources = resources, count = 1, isCapped = false)
        )
    }

    @Test
    fun many_isPlural() {
        assertEquals(
            "8 results",
            formatResultCount(resources = resources, count = 8, isCapped = false)
        )
    }

    @Test
    fun capped_showsPlusSuffix() {
        assertEquals(
            "50+ results",
            formatResultCount(resources = resources, count = 50, isCapped = true)
        )
    }
}
