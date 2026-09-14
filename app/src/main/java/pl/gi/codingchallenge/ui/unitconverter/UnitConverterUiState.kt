package pl.gi.codingchallenge.ui.unitconverter

import pl.gi.codingchallenge.shared.catfact.CatFact
import pl.gi.codingchallenge.shared.history.ConversionRecord
import pl.gi.codingchallenge.shared.history.ConversionType

// Single immutable snapshot the screen renders from - same "one UiState StateFlow"
// shape as AutocompleteUiState, just simpler since conversion here is synchronous
// (no loading/error states needed).
data class UnitConverterUiState(
    val platformName: String,
    val inputText: String,
    val selectedType: ConversionType,
    val result: Double?,
    val history: List<ConversionRecord>,
    // Ktor spike result - reuses this screen rather than a new debug entry point.
    val catFact: CatFact?,
)
