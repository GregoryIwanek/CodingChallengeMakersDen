package pl.gi.codingchallenge.ui.unitconverter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.gi.codingchallenge.shared.catfact.CatFactApi
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

// This ViewModel is the only place :app and :shared actually touch. Hilt still
// provides the ViewModel itself (standard :app-side DI), but everything it calls
// into on the :shared side - the repository and the pure conversion functions - is
// constructor-called/imported directly, since no DI framework reaches into :shared
// yet (Koin arrives in a later step).
@HiltViewModel
class UnitConverterViewModel @Inject constructor() : ViewModel() {

    private val historyRepository: ConversionHistoryRepository = InMemoryConversionHistoryRepository()

    // Real OkHttp-backed engine - CatFactApiTest injects a MockEngine instead, the
    // same constructor seam used here for the real thing.
    private val catFactApi = CatFactApi(OkHttp.create())

    private val _uiState = MutableStateFlow(
        UnitConverterUiState(
            platformName = platformName(),
            inputText = "",
            selectedType = ConversionType.CELSIUS_TO_FAHRENHEIT,
            result = null,
            history = emptyList(),
            catFact = null,
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

    fun onLoadCatFactClicked() {
        viewModelScope.launch {
            val fact = catFactApi.fetchCatFact()
            _uiState.update { it.copy(catFact = fact) }
        }
    }

    private fun ConversionType.convert(input: Double): Double = when (this) {
        ConversionType.CELSIUS_TO_FAHRENHEIT -> celsiusToFahrenheit(input)
        ConversionType.FAHRENHEIT_TO_CELSIUS -> fahrenheitToCelsius(input)
        ConversionType.KILOMETERS_TO_MILES -> kilometersToMiles(input)
        ConversionType.MILES_TO_KILOMETERS -> milesToKilometers(input)
    }
}
