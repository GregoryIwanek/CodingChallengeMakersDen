package pl.gi.codingchallenge.domain

import app.cash.turbine.test
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pl.gi.codingchallenge.domain.model.SearchResultItem

@OptIn(ExperimentalCoroutinesApi::class)
class SearchAutocompleteUseCaseTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeGitHubSearchRepository
    private lateinit var useCase: SearchAutocompleteUseCase
    private lateinit var query: MutableStateFlow<String>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeRepo = FakeGitHubSearchRepository()
        useCase = SearchAutocompleteUseCase(fakeRepo)
        query = MutableStateFlow("")
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `query under 3 chars stays Idle and never calls repository`() = runTest(dispatcher) {
        useCase(query).test {
            assertEquals(AutocompleteUiState.Idle, awaitItem())

            query.value = "ab"
            advanceTimeBy(500)

            // "" -> "ab" is still a distinct query, so flatMapLatest legitimately
            // re-runs and re-emits Idle — distinctUntilChanged dedupes the query
            // string, not the resulting state. What actually matters here is that
            // the repository is never called for a sub-3-char query.
            assertEquals(AutocompleteUiState.Idle, awaitItem())
            assertEquals(0, fakeRepo.searchCallCount)
        }
    }

    @Test
    fun `rapid typing only triggers a search for the final debounced query`() = runTest(dispatcher) {
        fakeRepo.enqueueResult(query = "kot", results = listOf(fakeRepo.sampleRepo("kotlin")))

        useCase(query).test {
            assertEquals(AutocompleteUiState.Idle, awaitItem())

            query.value = "k"
            query.value = "ko"
            query.value = "kot" // only this one should survive debounce
            advanceTimeBy(400) // > debounce window

            assertEquals(AutocompleteUiState.Loading, awaitItem())
            val success = awaitItem() as AutocompleteUiState.Success
            assertEquals(1, success.items.size)
            assertEquals(1, fakeRepo.searchCallCount) // proves debounce collapsed 3 keystrokes into 1 call
        }
    }

    @Test
    fun `new query cancels in-flight previous search (flatMapLatest)`() = runTest(dispatcher) {
        fakeRepo.enqueueDelayedResult(
            "first",
            delayMs = 1000,
            results = listOf(fakeRepo.sampleRepo("first-repo")),
        )
        fakeRepo.enqueueResult(query = "second", results = listOf(fakeRepo.sampleRepo("second-repo")))

        useCase(query).test {
            awaitItem() // Idle
            query.value = "first"
            advanceTimeBy(400) // fires "first" search, still in flight (1000ms delay)
            awaitItem() // Loading

            query.value = "second"
            advanceTimeBy(400) // debounce fires "second" before "first" resolves

            awaitItem() // Loading (for "second")
            val result = awaitItem() as AutocompleteUiState.Success
            assertEquals("second-repo", (result.items.first() as SearchResultItem.RepoResult).name)
            // "first" result, if it ever arrived, must never overwrite this — flatMapLatest guarantees it
        }
    }

    @Test
    fun `empty results maps to Empty state`() = runTest(dispatcher) {
        fakeRepo.enqueueResult(query = "zzz", results = emptyList())
        useCase(query).test {
            awaitItem() // Idle
            query.value = "zzz"
            advanceTimeBy(400)
            awaitItem() // Loading
            assertEquals(AutocompleteUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `repository failure maps to Error state`() = runTest(dispatcher) {
        fakeRepo.enqueueError(query = "boom", error = IOException("network down"))
        useCase(query).test {
            awaitItem() // Idle
            query.value = "boom"
            advanceTimeBy(400)
            awaitItem() // Loading
            val error = awaitItem() as AutocompleteUiState.Error
            assertEquals("network down", error.message)
        }
    }
}
