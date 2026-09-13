package pl.gi.codingchallenge.ui.unitconverter

import pl.gi.codingchallenge.shared.history.ConversionRecord
import pl.gi.codingchallenge.shared.history.ConversionType

data class UnitConverterUiState(
    val platformName: String,
    val inputText: String,
    val selectedType: ConversionType,
    val result: Double?,
    val history: List<ConversionRecord>,
)
