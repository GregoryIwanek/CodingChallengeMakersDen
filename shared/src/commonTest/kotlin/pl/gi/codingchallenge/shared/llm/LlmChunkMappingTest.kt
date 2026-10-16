package pl.gi.codingchallenge.shared.llm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LlmChunkMappingTest {

    @Test
    fun `unnamed frame becomes text`() {
        val chunk: LlmChunk? = toLlmChunk(event = null, data = "Ok")

        assertEquals(LlmChunk.Text(value = "Ok"), chunk)
    }

    // Ktor's SSE client rejoins the proxy's multiple data: lines with \r\n. Without
    // the normalisation in toLlmChunk, every newline in the summary would carry a
    // stray \r.
    @Test
    fun `multi-line text keeps lf line breaks`() {
        val chunk: LlmChunk? = toLlmChunk(event = null, data = "line1\r\nline2")

        assertEquals(LlmChunk.Text(value = "line1\nline2"), chunk)
    }

    // "message" is the SSE spec's default event name, so it must map like an unnamed frame.
    @Test
    fun `message event becomes text`() {
        val chunk: LlmChunk? = toLlmChunk(event = "message", data = "Ok")

        assertEquals(LlmChunk.Text(value = "Ok"), chunk)
    }

    @Test
    fun `text frame without data is skipped`() {
        val chunk: LlmChunk? = toLlmChunk(event = null, data = null)

        assertNull(chunk)
    }

    @Test
    fun `stop event becomes finished with its reason`() {
        val chunk: LlmChunk? = toLlmChunk(event = "stop", data = "end_turn")

        assertEquals(LlmChunk.Finished(stopReason = "end_turn"), chunk)
    }

    @Test
    fun `truncated event becomes truncated`() {
        val chunk: LlmChunk? = toLlmChunk(event = "truncated", data = null)

        assertEquals(LlmChunk.Truncated, chunk)
    }

    @Test
    fun `error event with content becomes failed`() {
        val chunk: LlmChunk? = toLlmChunk(event = "error", data = "readme_not_found")

        assertEquals(LlmChunk.Failed(code = "readme_not_found"), chunk)
    }

    // Unknown event names are skipped rather than shown as text, so the proxy can
    // add new events without older app versions printing them as summary.
    @Test
    fun `unknown event is skipped`() {
        val chunk: LlmChunk? = toLlmChunk(event = "ping", data = "keep-alive")

        assertNull(chunk)
    }
}
