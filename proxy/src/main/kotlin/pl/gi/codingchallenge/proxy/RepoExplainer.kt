package pl.gi.codingchallenge.proxy

import com.anthropic.client.AnthropicClient
import com.anthropic.models.beta.messages.BetaOutputConfig
import com.anthropic.models.beta.messages.MessageCreateParams
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/** The proxy's own event type, so the route maps these to SSE frames without knowing SDK classes. */
sealed interface ExplainEvent {
    data class Text(val value: String) : ExplainEvent

    /** `end_turn`, `max_tokens` or `refusal`, so the client can tell them apart. */
    data class Stop(val reason: String) : ExplainEvent
}

/** Turns a README into a stream of summary events. Tests pass a lambda, so they need no key or network. */
fun interface RepoExplainer {
    fun explain(readme: String): Flow<ExplainEvent>
}

class ClaudeRepoExplainer(private val claude: AnthropicClient) : RepoExplainer {
    // The SDK stream is blocking, so it's read on IO. Cancelling the collector (the client
    // disconnected) ends the flow at the next emit, and use {} closes the SDK stream, so
    // tokens nobody will read stop being generated.
    override fun explain(readme: String): Flow<ExplainEvent> = flow {
        claude.beta().messages().createStreaming(params(readme)).use { response ->
            for (event in response.stream().iterator()) {
                event
                    .contentBlockDelta()
                    .flatMap { it.delta().text() }
                    .getOrNull()
                    ?.let { emit(ExplainEvent.Text(it.text())) }
                event
                    .messageDelta()
                    .flatMap { it.delta().stopReason() }
                    .getOrNull()
                    ?.let { emit(ExplainEvent.Stop(it.toString())) }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun params(readme: String): MessageCreateParams = MessageCreateParams
        .builder()
        .model(MODEL)
        .maxTokens(MAX_TOKENS)
        // Opus 5.5 always thinks; low effort keeps time to first token short for a summary.
        .outputConfig(BetaOutputConfig.builder().effort(BetaOutputConfig.Effort.LOW).build())
        // On a refusal, the API retries on a model chosen by refusal category.
        .addBeta(FALLBACK_BETA)
        .fallbacksDefault()
        .system(SYSTEM_PROMPT)
        .addUserMessage("<readme>\n$readme\n</readme>")
        .build()

    private companion object {
        const val MODEL = "claude-opus-5-5"
        const val MAX_TOKENS = 4_000L
        const val FALLBACK_BETA = "server-side-fallback-2026-07-01"
        val SYSTEM_PROMPT =
            """
            You explain GitHub repositories to Android developers. Given a README, write:
            one sentence on what it is, then 3 short bullets on when to use it, then one line
            on alternatives. Plain Markdown, no headings, under 120 words.
            """.trimIndent()
    }
}
