package pl.gi.codingchallenge.shared.history

// One value per UnitConverter function; `label` doubles as the human-readable
// string the :app UI shows directly, so the UI never re-derives it.
enum class ConversionType(val label: String) {
    CELSIUS_TO_FAHRENHEIT("°C → °F"),
    FAHRENHEIT_TO_CELSIUS("°F → °C"),
    KILOMETERS_TO_MILES("km → mi"),
    MILES_TO_KILOMETERS("mi → km"),
}

// The "domain model" for the history feature: one entry per conversion performed.
// Left open by the original guide - these are the fields this implementation chose.
data class ConversionRecord(
    val type: ConversionType,
    val input: Double,
    val output: Double,
)
