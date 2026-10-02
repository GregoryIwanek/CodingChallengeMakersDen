package pl.gi.codingchallenge.feature.detail

import android.content.res.Resources
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Paparazzi is used only for its Android [Resources] on the JVM, so the
 * plurals lookup can be tested without Robolectric or a device.
 */
class FormatStarsCountTest {

    @get:Rule
    val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6.copy(locale = "en")
    )

    private val resources: Resources
        get() = paparazzi.context.resources

    @Test
    fun zero_isPlural() {
        assertEquals("0 stars", formatStarsCount(resources = resources, stars = 0))
    }

    @Test
    fun one_isSingular() {
        assertEquals("1 star", formatStarsCount(resources = resources, stars = 1))
    }

    @Test
    fun thousands_areGrouped() {
        assertEquals("48,000 stars", formatStarsCount(resources = resources, stars = 48000))
    }
}
