package pl.gi.codingchallenge.proxy

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.routing.routing
import io.ktor.server.sse.SSE
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlin.coroutines.cancellation.CancellationException

const val MAX_README_CHARS = 40_000

// GitHub owner and repo name characters only, so the value is safe to put in a URL path.
private val REPO_PATTERN = Regex("^[A-Za-z0-9-]+/[A-Za-z0-9._-]+$")

/**
 * `GET /explain?repo=owner/name` streams a README summary as SSE: `data:` text frames, then
 * one `stop` frame. A failure becomes one `error` frame, because the SSE response has already
 * started and its HTTP status can't carry it.
 */
fun Application.proxyModule(readmes: ReadmeSource, explainer: RepoExplainer) {
    install(SSE)
    routing {
        sse("/explain") {
            val repo =
                call.parameters["repo"]?.takeIf(REPO_PATTERN::matches)
                    ?: return@sse sendError("invalid_repo")
            try {
                val readme = readmes.fetch(repo) ?: return@sse sendError("readme_not_found")
                if (readme.length > MAX_README_CHARS) {
                    send(
                        ServerSentEvent(
                            data = "README cut to $MAX_README_CHARS characters",
                            event = "truncated"
                        )
                    )
                }
                explainer.explain(readme.take(MAX_README_CHARS)).collect { event ->
                    when (event) {
                        is ExplainEvent.Text -> send(ServerSentEvent(data = event.value))
                        is ExplainEvent.Stop -> send(
                            ServerSentEvent(data = event.reason, event = "stop")
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                this@proxyModule.log.warn("Explaining $repo failed", e)
                sendError("upstream_error")
            }
        }
    }
}

private suspend fun ServerSSESession.sendError(code: String) =
    send(ServerSentEvent(data = code, event = "error"))
