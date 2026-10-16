package pl.gi.codingchallenge.feature.detail

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import pl.gi.codingchallenge.shared.llm.LlmChunk
import pl.gi.codingchallenge.shared.llm.LlmClient

/**
 * Asserts the final state after advanceUntilIdle(), not every step: StateFlow conflates quick
 * updates, so expecting each intermediate Streaming value would be flaky.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ExplainRepoViewModelTest {

    private val dispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `explain accumulates text then finishes`() = runTest(dispatcher) {
        val viewModel: ExplainRepoViewModel = viewModelStreaming(
            chunks = flowOf(
                LlmChunk.Text(value = "Ok"),
                LlmChunk.Text(value = "Http"),
                LlmChunk.Finished(stopReason = "end_turn")
            )
        )

        viewModel.explain(fullName = REPO)
        advanceUntilIdle()

        assertEquals(ExplainUiState.Done(text = "OkHttp"), viewModel.state.value)
    }

    @Test
    fun `failure mid-stream keeps the partial text`() = runTest(dispatcher) {
        val viewModel: ExplainRepoViewModel = viewModelStreaming(
            chunks = flow {
                emit(LlmChunk.Text(value = "Ok"))
                throw IOException("boom")
            }
        )

        viewModel.explain(fullName = REPO)
        advanceUntilIdle()

        assertEquals(
            ExplainUiState.Failed(partial = "Ok", reason = ExplainFailure.Network),
            viewModel.state.value
        )
    }

    @Test
    fun `stop after some text marks the summary as stopped by user`() = runTest(dispatcher) {
        val viewModel: ExplainRepoViewModel = viewModelStreaming(
            chunks = flow {
                emit(LlmChunk.Text(value = "Ok"))
                awaitCancellation()
            }
        )

        viewModel.explain(fullName = REPO)
        advanceUntilIdle()
        assertEquals(ExplainUiState.Streaming(text = "Ok"), viewModel.state.value)

        viewModel.stop()
        advanceUntilIdle()

        assertEquals(
            ExplainUiState.Done(text = "Ok", stoppedByUser = true),
            viewModel.state.value
        )
    }

    private fun viewModelStreaming(chunks: Flow<LlmChunk>): ExplainRepoViewModel =
        ExplainRepoViewModel(llm = FakeLlmClient(chunks = chunks))

    private class FakeLlmClient(private val chunks: Flow<LlmChunk>) : LlmClient {
        override val isEnabled: Boolean = true
        override fun explainRepo(fullName: String): Flow<LlmChunk> = chunks
    }

    private companion object {
        const val REPO: String = "square/okhttp"
    }
}
