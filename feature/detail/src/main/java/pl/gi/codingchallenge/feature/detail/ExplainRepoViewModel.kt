package pl.gi.codingchallenge.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import pl.gi.codingchallenge.shared.llm.LlmChunk
import pl.gi.codingchallenge.shared.llm.LlmClient

private const val STOP_REASON_REFUSAL: String = "refusal"
private const val ERROR_CODE_README_NOT_FOUND: String = "readme_not_found"

@HiltViewModel
class ExplainRepoViewModel @Inject constructor(private val llm: LlmClient) : ViewModel() {

    private val _state: MutableStateFlow<ExplainUiState> = MutableStateFlow(ExplainUiState.Idle)
    val state: StateFlow<ExplainUiState> = _state.asStateFlow()

    val isAvailable: Boolean = llm.isEnabled

    private var job: Job? = null

    fun explain(fullName: String) {
        job?.cancel()
        _state.value = ExplainUiState.WaitingForFirstToken
        job = viewModelScope.launch {
            val text = StringBuilder()
            try {
                llm.explainRepo(fullName = fullName).collect { chunk ->
                    _state.value = when (chunk) {
                        is LlmChunk.Text -> {
                            text.append(chunk.value)
                            ExplainUiState.Streaming(text = text.toString())
                        }
                        is LlmChunk.Finished -> if (chunk.stopReason == STOP_REASON_REFUSAL) {
                            ExplainUiState.Refused(partial = text.toString())
                        } else {
                            ExplainUiState.Done(text = text.toString())
                        }
                        is LlmChunk.Failed -> ExplainUiState.Failed(
                            partial = text.toString(),
                            reason = failureFor(code = chunk.code)
                        )
                        // Still a valid summary; showing a note is a later polish.
                        LlmChunk.Truncated -> _state.value
                    }
                }
                // The proxy always ends with a stop or error frame; without one, the
                // connection was cut.
                val last: ExplainUiState = _state.value
                if (last is ExplainUiState.Streaming ||
                    last is ExplainUiState.WaitingForFirstToken
                ) {
                    _state.value = ExplainUiState.Failed(
                        partial = text.toString(),
                        reason = ExplainFailure.Network
                    )
                }
            } catch (e: CancellationException) {
                // Only a Stop leaves this job current. A Retry has already replaced `job` and
                // set WaitingForFirstToken, which this late catch must not overwrite.
                if (job == coroutineContext.job) {
                    _state.value = if (text.isEmpty()) {
                        ExplainUiState.Idle
                    } else {
                        ExplainUiState.Done(text = text.toString(), stoppedByUser = true)
                    }
                }
                throw e
            } catch (e: Exception) {
                // Keep the partial text: losing half an answer is worse than showing it.
                _state.value = ExplainUiState.Failed(
                    partial = text.toString(),
                    reason = ExplainFailure.Network
                )
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    private fun failureFor(code: String): ExplainFailure = if (code ==
        ERROR_CODE_README_NOT_FOUND
    ) {
        ExplainFailure.NoReadme
    } else {
        ExplainFailure.ServiceError
    }
}
