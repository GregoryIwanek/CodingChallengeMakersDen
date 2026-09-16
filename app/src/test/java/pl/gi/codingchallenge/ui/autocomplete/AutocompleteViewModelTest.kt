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
import pl.gi.codingchallenge.shared.domain.model.QueryRequest
import pl.gi.codingchallenge.shared.domain.model.SearchOutcome
import pl.gi.codingchallenge.shared.domain.model.SearchResultItem
import pl.gi.codingchallenge.shared.domain.usecase.SearchAutocompleteUseCase

/**
 * Covers only the ViewModel's own wiring (forwards queries, exposes
 * use-case emissions) — debounce/cancellation is SearchAutocompleteUseCaseTest's job.
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
    fun `uiState maps every SearchOutcome to its AutocompleteUiState`() = runTest(dispatcher) {
        val useCase = mockk<SearchAutocompleteUseCase>()
        val nonEmptyResults = listOf(
            SearchResultItem.UserResult(
                id = "1", login = "octocat", avatarUrl = null, htmlUrl = "https://github.com/octocat",
            ),
        )
        every { useCase(any()) } returns flowOf(
            SearchOutcome.Loading,
            SearchOutcome.Success(nonEmptyResults),
            SearchOutcome.Success(emptyList()),
            SearchOutcome.Failure("boom"),
        )

        val viewModel = AutocompleteViewModel(useCase)

        viewModel.uiState.test {
            assertEquals(AutocompleteUiState.Idle, awaitItem()) // stateIn's initial value
            assertEquals(AutocompleteUiState.Loading, awaitItem())
            assertEquals(AutocompleteUiState.Success(nonEmptyResults), awaitItem())
            // No separate Empty outcome in the domain layer — this is where
            // an empty Success list becomes the Empty UI state.
            assertEquals(AutocompleteUiState.Empty, awaitItem())
            assertEquals(AutocompleteUiState.Error("boom"), awaitItem())
        }
    }

    @Test
    fun `onQueryChanged forwards the new query into the use case's input flow`() = runTest(dispatcher) {
        val useCase = mockk<SearchAutocompleteUseCase>()
        val requestsSlot = slot<Flow<QueryRequest>>()
        every { useCase(capture(requestsSlot)) } returns flowOf()

        val viewModel = AutocompleteViewModel(useCase)
        viewModel.uiState.test { awaitItem() } // subscribe once so stateIn actually invokes the use case
        advanceUntilIdle()

        viewModel.onQueryChanged("kotlin")

        assertEquals("kotlin", (requestsSlot.captured as MutableStateFlow<QueryRequest>).value.text)
    }

    @Test
    fun `retry bumps attempt but leaves text unchanged, forwarding into the use case`() =
        runTest(dispatcher) {
            val useCase = mockk<SearchAutocompleteUseCase>()
            val requestsSlot = slot<Flow<QueryRequest>>()
            every { useCase(capture(requestsSlot)) } returns flowOf()

            val viewModel = AutocompleteViewModel(useCase)
            viewModel.uiState.test { awaitItem() } // subscribe once so stateIn actually invokes the use case
            advanceUntilIdle()

            viewModel.onQueryChanged("kotlin")
            val requests = requestsSlot.captured as MutableStateFlow<QueryRequest>
            assertEquals(0, requests.value.attempt)

            viewModel.retry()

            assertEquals("kotlin", requests.value.text)
            assertEquals(1, requests.value.attempt)
        }
}
