package pl.gi.codingchallenge.ui.autocomplete

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pl.gi.codingchallenge.domain.AutocompleteUiState
import pl.gi.codingchallenge.domain.SearchAutocompleteUseCase

/**
 * AutocompleteViewModel is a thin adapter — its
 * own debounce/cancellation logic already lives in, and is tested by,
 * SearchAutocompleteUseCaseTest. What's worth verifying here is only the
 * ViewModel's own wiring: it forwards queries into the use case and
 * exposes whatever the use case emits. SearchAutocompleteUseCase is a
 * concrete class, mocked directly with MockK — no interface needed for
 * that (see the "should the use case be an interface" discussion).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AutocompleteViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `uiState starts as Idle before the use case emits anything`() = runTest(dispatcher) {
        val useCase = mockk<SearchAutocompleteUseCase>()
        every { useCase(any()) } returns flowOf()

        val viewModel = AutocompleteViewModel(useCase)

        assertEquals(AutocompleteUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `uiState reflects whatever the use case emits`() = runTest(dispatcher) {
        val useCase = mockk<SearchAutocompleteUseCase>()
        val success = AutocompleteUiState.Success(emptyList())
        every { useCase(any()) } returns flowOf(AutocompleteUiState.Loading, success)

        val viewModel = AutocompleteViewModel(useCase)

        viewModel.uiState.test {
            assertEquals(AutocompleteUiState.Idle, awaitItem()) // stateIn's initial value
            assertEquals(AutocompleteUiState.Loading, awaitItem())
            assertEquals(success, awaitItem())
        }
    }

    @Test
    fun `onQueryChanged forwards the new query into the use case's input flow`() = runTest(dispatcher) {
        val useCase = mockk<SearchAutocompleteUseCase>()
        val queriesSlot = slot<Flow<String>>()
        every { useCase(capture(queriesSlot)) } returns flowOf()

        val viewModel = AutocompleteViewModel(useCase)
        viewModel.uiState.test { awaitItem() } // subscribe once so stateIn actually invokes the use case
        advanceUntilIdle()

        viewModel.onQueryChanged("kotlin")

        assertEquals("kotlin", (queriesSlot.captured as MutableStateFlow<String>).value)
    }
}
