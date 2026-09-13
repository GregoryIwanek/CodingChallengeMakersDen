package pl.gi.codingchallenge.ui.unitconverter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.gi.codingchallenge.shared.converter.celsiusToFahrenheit
import pl.gi.codingchallenge.shared.converter.fahrenheitToCelsius
import pl.gi.codingchallenge.shared.converter.kilometersToMiles
import pl.gi.codingchallenge.shared.converter.milesToKilometers
import pl.gi.codingchallenge.shared.history.ConversionHistoryRepository
import pl.gi.codingchallenge.shared.history.ConversionRecord
import pl.gi.codingchallenge.shared.history.ConversionType
import pl.gi.codingchallenge.shared.history.InMemoryConversionHistoryRepository
import pl.gi.codingchallenge.shared.platformName
import javax.inject.Inject

@HiltViewModel
class UnitConverterViewModel @Inject constructor() : ViewModel() {

    // Constructor-called directly - no DI framework wires :shared's classes
    // in yet (kmp-interview-prep step 4 introduces Koin for this).
    private val historyRepository: ConversionHistoryRepository = InMemoryConversionHistoryRepository()

    private val _uiState = MutableStateFlow(
        UnitConverterUiState(
            platformName = platformName(),
            inputText = "",
            selectedType = ConversionType.CELSIUS_TO_FAHRENHEIT,
            result = null,
            history = emptyList(),
        ),
    )
    val uiState: StateFlow<UnitConverterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            historyRepository.history.collect { history ->
                _uiState.update { it.copy(history = history) }
            }
        }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onTypeSelected(type: ConversionType) {
        _uiState.update { it.copy(selectedType = type) }
    }

    fun onConvertClicked() {
        val state = _uiState.value
        val input = state.inputText.toDoubleOrNull() ?: return
        val output = state.selectedType.convert(input)

        _uiState.update { it.copy(result = output) }
        viewModelScope.launch {
            historyRepository.record(ConversionRecord(state.selectedType, input, output))
        }
    }

    private fun ConversionType.convert(input: Double): Double = when (this) {
        ConversionType.CELSIUS_TO_FAHRENHEIT -> celsiusToFahrenheit(input)
        ConversionType.FAHRENHEIT_TO_CELSIUS -> fahrenheitToCelsius(input)
        ConversionType.KILOMETERS_TO_MILES -> kilometersToMiles(input)
        ConversionType.MILES_TO_KILOMETERS -> milesToKilometers(input)
    }
}
