package pl.gi.codingchallenge.proxy

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

    embeddedServer(Netty, port = port) {
    }.start(wait = true)
}

private const val DEFAULT_PORT = 8080
