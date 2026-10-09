package pl.gi.codingchallenge.proxy

import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

class ExplainRoutesTest {
    /** One received SSE frame; `event` is null for plain text frames. */
    private data class Frame(val event: String?, val data: String?)

    private val okhttpReadme = ReadmeSource { "# OkHttp\nAn HTTP client." }

    private fun streaming(vararg events: ExplainEvent) = RepoExplainer { flowOf(*events) }

    private fun explain(
        repo: String? = "square/okhttp",
        readmes: ReadmeSource = okhttpReadme,
        explainer: RepoExplainer
    ): List<Frame> {
        var frames = emptyList<Frame>()
        testApplication {
            application { proxyModule(readmes, explainer) }
            val client = createClient { install(SSE) }
            val path = if (repo == null) "/explain" else "/explain?repo=$repo"
            client.sse(path) {
                frames = incoming.map { Frame(it.event, it.data) }.toList()
            }
        }
        return frames
    }

    @Test
    fun `streams text frames in order, then the stop reason`() {
        val frames =
            explain(
                explainer = streaming(
                    ExplainEvent.Text("Ok"),
                    ExplainEvent.Text("Http"),
                    ExplainEvent.Stop("end_turn")
                )
            )

        assertEquals(
            listOf(Frame(null, "Ok"), Frame(null, "Http"), Frame("stop", "end_turn")),
            frames
        )
    }

    @Test
    fun `passes a refusal through as the stop reason`() {
        val frames =
            explain(
                explainer = streaming(ExplainEvent.Stop("refusal"))
            )

        assertEquals(listOf(Frame("stop", "refusal")), frames)
    }

    @Test
    fun `splits a multi-line text frame into one data line per line`() {
        // Checked on the raw body: Ktor's SSE client rejoins data lines with "\r\n", not the
        // spec's "\n", so a client-side assertion would test Ktor rather than this server.
        var body = ""
        testApplication {
            application { proxyModule(okhttpReadme, streaming(ExplainEvent.Text("- one\n- two"))) }
            body = client.get("/explain?repo=square/okhttp").bodyAsText()
        }

        assertEquals("data: - one\r\ndata: - two\r\n\r\n", body)
    }

    @Test
    fun `rejects a missing or malformed repo without fetching`() {
        var fetched = false
        val readmes =
            ReadmeSource {
                fetched = true
                "unused"
            }

        for (repo in listOf(null, "okhttp", "square/okhttp/extra", "..%2F..%2Fusers")) {
            val frames = explain(repo, readmes, streaming())
            assertEquals(listOf(Frame("error", "invalid_repo")), frames, "repo=$repo")
        }
        assertFalse(fetched)
    }

    @Test
    fun `reports a missing README without calling the model`() {
        var modelCalled = false
        val explainer =
            RepoExplainer {
                modelCalled = true
                flowOf()
            }

        val frames = explain(readmes = { null }, explainer = explainer)

        assertEquals(listOf(Frame("error", "readme_not_found")), frames)
        assertFalse(modelCalled)
    }

    @Test
    fun `cuts a long README and says so`() {
        var received: String? = null
        val explainer =
            RepoExplainer { readme ->
                received = readme
                flowOf(ExplainEvent.Stop("end_turn"))
            }

        val frames =
            explain(
                readmes = { "x".repeat(MAX_README_CHARS + 1) },
                explainer = explainer
            )

        assertEquals("truncated", frames.first().event)
        assertEquals(MAX_README_CHARS, received?.length)
    }

    @Test
    fun `does not flag a README at exactly the limit`() {
        val frames =
            explain(
                readmes = { "x".repeat(MAX_README_CHARS) },
                explainer = streaming(ExplainEvent.Stop("end_turn"))
            )

        assertNull(frames.firstOrNull { it.event == "truncated" })
    }

    @Test
    fun `keeps text sent before a model failure, then reports the error`() {
        val explainer =
            RepoExplainer {
                flow {
                    emit(ExplainEvent.Text("Partial"))
                    error("connection reset")
                }
            }

        val frames = explain(explainer = explainer)

        assertEquals(listOf(Frame(null, "Partial"), Frame("error", "upstream_error")), frames)
    }

    @Test
    fun `reports a README fetch failure as an upstream error`() {
        val frames =
            explain(
                readmes = { error("GitHub returned 503") },
                explainer = streaming()
            )

        assertEquals(listOf(Frame("error", "upstream_error")), frames)
    }
}
