package pl.gi.codingchallenge.shared.converter

import kotlin.test.Test
import kotlin.test.assertEquals

class UnitConverterTest {

    @Test
    fun celsiusToFahrenheit_freezingPoint() {
        assertEquals(32.0, celsiusToFahrenheit(0.0), absoluteTolerance = 0.01)
    }

    @Test
    fun fahrenheitToCelsius_freezingPoint() {
        assertEquals(0.0, fahrenheitToCelsius(32.0), absoluteTolerance = 0.01)
    }

    @Test
    fun kilometersToMiles_oneKilometer() {
        assertEquals(0.621371, kilometersToMiles(1.0), absoluteTolerance = 0.0001)
    }

    @Test
    fun milesToKilometers_oneMile() {
        assertEquals(1.609344, milesToKilometers(1.0), absoluteTolerance = 0.001)
    }
}
