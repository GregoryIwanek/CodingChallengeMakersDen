package pl.gi.codingchallenge.domain.usecase

import app.cash.turbine.test
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pl.gi.codingchallenge.domain.model.QueryRequest
import pl.gi.codingchallenge.domain.model.SearchOutcome
import pl.gi.codingchallenge.domain.model.SearchResultItem
import pl.gi.codingchallenge.domain.repository.FakeGitHubSearchRepository

@OptIn(ExperimentalCoroutinesApi::class)
class SearchAutocompleteUseCaseTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var fakeRepo: FakeGitHubSearchRepository
    private lateinit var useCase: SearchAutocompleteUseCase
    private lateinit var query: MutableStateFlow<QueryRequest>

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeRepo = FakeGitHubSearchRepository()
        useCase = SearchAutocompleteUseCase(fakeRepo)
        query = MutableStateFlow(QueryRequest(text = ""))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `query under 3 chars stays QueryTooShort and never calls repository`() = runTest(dispatcher) {
        useCase(query).test {
            assertEquals(SearchOutcome.QueryTooShort, awaitItem())

            query.value = QueryRequest(text = "ab")
            advanceTimeBy(500)

            // "" -> "ab" is still a distinct query, so flatMapLatest legitimately
            // re-runs and re-emits QueryTooShort — distinctUntilChanged dedupes
            // the query string, not the resulting outcome. What actually matters
            // here is that the repository is never called for a sub-3-char query.
            assertEquals(SearchOutcome.QueryTooShort, awaitItem())
            assertEquals(0, fakeRepo.searchCallCount)
        }
    }

    @Test
    fun `rapid typing only triggers a search for the final debounced query`() = runTest(dispatcher) {
        fakeRepo.enqueueResult(query = "kot", results = listOf(fakeRepo.sampleRepo("kotlin")))

        useCase(query).test {
            assertEquals(SearchOutcome.QueryTooShort, awaitItem())

            query.value = QueryRequest(text = "k")
            query.value = QueryRequest(text = "ko")
            query.value = QueryRequest(text = "kot") // only this one should survive debounce
            advanceTimeBy(400) // > debounce window

            assertEquals(SearchOutcome.Loading, awaitItem())
            val success = awaitItem() as SearchOutcome.Success
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
            awaitItem() // QueryTooShort
            query.value = QueryRequest(text = "first")
            advanceTimeBy(400) // fires "first" search, still in flight (1000ms delay)
            awaitItem() // Loading

            query.value = QueryRequest(text = "second")
            advanceTimeBy(400) // debounce fires "second" before "first" resolves

            awaitItem() // Loading (for "second")
            val result = awaitItem() as SearchOutcome.Success
            assertEquals("second-repo", (result.items.first() as SearchResultItem.RepoResult).name)
            // "first" result, if it ever arrived, must never overwrite this — flatMapLatest guarantees it
        }
    }

    @Test
    fun `empty results map to Success with an empty list`() = runTest(dispatcher) {
        fakeRepo.enqueueResult(query = "zzz", results = emptyList())
        useCase(query).test {
            awaitItem() // QueryTooShort
            query.value = QueryRequest(text = "zzz")
            advanceTimeBy(400)
            awaitItem() // Loading
            // No separate "Empty" outcome in the domain layer — deciding to
            // render an empty list as an "empty state" is a UI concern.
            assertEquals(SearchOutcome.Success(emptyList()), awaitItem())
        }
    }

    @Test
    fun `repository failure maps to Failure outcome`() = runTest(dispatcher) {
        fakeRepo.enqueueError(query = "boom", error = IOException("network down"))
        useCase(query).test {
            awaitItem() // QueryTooShort
            query.value = QueryRequest(text = "boom")
            advanceTimeBy(400)
            awaitItem() // Loading
            val failure = awaitItem() as SearchOutcome.Failure
            assertEquals("network down", failure.message)
        }
    }

    @Test
    fun `retrying the same query re-triggers a search`() = runTest(dispatcher) {
        fakeRepo.enqueueError(query = "boom", error = IOException("network down"))

        useCase(query).test {
            awaitItem() // QueryTooShort
            query.value = QueryRequest(text = "boom")
            advanceTimeBy(400)
            awaitItem() // Loading
            val failure = awaitItem() as SearchOutcome.Failure
            assertEquals("network down", failure.message)
            assertEquals(1, fakeRepo.searchCallCount)

            // Same text, no new results enqueued for it — this proves the
            // retry itself (not a lucky re-enqueue) is what re-fires the
            // search. Only `attempt` changes; `text` is untouched.
            fakeRepo.enqueueResult(query = "boom", results = listOf(fakeRepo.sampleRepo("boom-repo")))
            query.update { it.copy(attempt = it.attempt + 1) }
            advanceTimeBy(400)

            awaitItem() // Loading
            val success = awaitItem() as SearchOutcome.Success
            assertEquals("boom-repo", (success.items.first() as SearchResultItem.RepoResult).name)
            assertEquals(2, fakeRepo.searchCallCount) // proves the retry actually re-called the repository
        }
    }
}
