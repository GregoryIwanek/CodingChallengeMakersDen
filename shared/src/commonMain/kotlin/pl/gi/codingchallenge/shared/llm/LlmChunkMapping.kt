package pl.gi.codingchallenge.shared.llm

/**
 * Maps one SSE frame to a chunk, or null to skip it. Unknown event names are skipped, so the
 * proxy can add new events without breaking older app versions.
 */
internal fun toLlmChunk(event: String?, data: String?): LlmChunk? = when (event) {
    // Unnamed frames are text. "message" is the SSE spec's default name.
    null, "message" -> data?.let { LlmChunk.Text(value = it.replace("\r\n", "\n")) }
    "stop" -> LlmChunk.Finished(stopReason = data.orEmpty())
    "error" -> LlmChunk.Failed(code = data.orEmpty())
    "truncated" -> LlmChunk.Truncated
    else -> null
}
