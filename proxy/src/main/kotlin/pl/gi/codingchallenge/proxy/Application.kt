package pl.gi.codingchallenge.proxy

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.system.exitProcess

fun main() {
    // Fail at startup, not on the first request, so a missing key is obvious.
    if (System.getenv("ANTHROPIC_API_KEY").isNullOrBlank()) {
        System.err.println("ANTHROPIC_API_KEY is not set. Export it in this shell, then run again.")
        exitProcess(1)
    }
    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

    val http = HttpClient(OkHttp)
    val claude = AnthropicOkHttpClient.fromEnv()

    embeddedServer(Netty, port = port) {
        proxyModule(GitHubReadmeSource(http), ClaudeRepoExplainer(claude))
        // Release both clients' connection pools and threads on a clean shutdown.
        monitor.subscribe(ApplicationStopped) {
            http.close()
            claude.close()
        }
    }.start(wait = true)
}

private const val DEFAULT_PORT = 8080
