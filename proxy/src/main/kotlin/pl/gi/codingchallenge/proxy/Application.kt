package pl.gi.codingchallenge.proxy

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlin.system.exitProcess

/**
 * Composition root: the only place that builds real clients and needs the API key.
 * Tests call [proxyModule] with fakes instead, so they never reach this.
 */
fun main() {
    // Fail at startup, not on the first request, so a missing key is obvious.
    if (System.getenv("ANTHROPIC_API_KEY").isNullOrBlank()) {
        System.err.println("ANTHROPIC_API_KEY is not set. Export it in this shell, then run again.")
        exitProcess(1)
    }
    val port = System.getenv("PORT")?.toIntOrNull() ?: DEFAULT_PORT

    val http = HttpClient(OkHttp)
    val claude = AnthropicOkHttpClient.fromEnv()

    // Loopback only: other machines on the network can't reach it and spend the credits.
    // The Android emulator still can, through 10.0.2.2, which maps to this host's loopback.
    embeddedServer(Netty, port = port, host = LOOPBACK_HOST) {
        proxyModule(GitHubReadmeSource(http), ClaudeRepoExplainer(claude))
        // Release both clients' connection pools and threads on a clean shutdown.
        monitor.subscribe(ApplicationStopped) {
            http.close()
            claude.close()
        }
    }.start(wait = true)
}

private const val DEFAULT_PORT = 8080
private const val LOOPBACK_HOST = "127.0.0.1"
