package pl.gi.codingchallenge.shared.llm

import kotlinx.coroutines.flow.Flow

/** One piece of a streamed answer, mapped from the proxy's SSE frames. */
sealed interface LlmChunk {
    /** Answer text to append. */
    data class Text(val value: String) : LlmChunk

    /** The model finished; `stopReason` is e.g. "end_turn", "max_tokens" or "refusal". */
    data class Finished(val stopReason: String) : LlmChunk

    /** The proxy reported a failure, e.g. "readme_not_found" or "upstream_error". */
    data class Failed(val code: String) : LlmChunk

    /** The README was cut before summarising; the summary is still valid. */
    data object Truncated : LlmChunk
}

interface LlmClient {
    /** False when no proxy URL is configured (release builds), so the UI can hide the feature. */
    val isEnabled: Boolean

    /** Streams a summary of `fullName`'s README. Cancelling the collector closes the connection. */
    fun explainRepo(fullName: String): Flow<LlmChunk>
}
