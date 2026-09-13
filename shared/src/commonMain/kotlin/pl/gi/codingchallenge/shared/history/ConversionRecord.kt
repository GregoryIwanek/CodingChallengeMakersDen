package pl.gi.codingchallenge.shared.history

enum class ConversionType(val label: String) {
    CELSIUS_TO_FAHRENHEIT("°C → °F"),
    FAHRENHEIT_TO_CELSIUS("°F → °C"),
    KILOMETERS_TO_MILES("km → mi"),
    MILES_TO_KILOMETERS("mi → km"),
}

data class ConversionRecord(
    val type: ConversionType,
    val input: Double,
    val output: Double,
)
