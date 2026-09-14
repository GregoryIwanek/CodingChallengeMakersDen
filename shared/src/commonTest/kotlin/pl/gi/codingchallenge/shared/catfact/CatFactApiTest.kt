package pl.gi.codingchallenge.shared.catfact

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CatFactApiTest {

    @Test
    fun fetchCatFact_parsesResponse() = runTest {
        val engine = MockEngine { request ->
            respond(
                content = """{"fact":"Cats sleep 70% of their lives.","length":32}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }

        val result = CatFactApi(engine).fetchCatFact()

        assertEquals("Cats sleep 70% of their lives.", result.fact)
        assertEquals(32, result.length)
    }
}
