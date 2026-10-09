# Weekend Plan: Building LLM-Powered UI on Android

**Goal:** by Sunday evening, be able to say *"I've built modern user interfaces powered by LLMs"*
and back it up with working code in this repo. That means more than calling an API and printing a
string: the code should show streaming, UI state for non-deterministic output, structured output
mapped to native Compose components, safe key handling, testing, and an on-device option.

**The worked example:** two features added to this app, both small enough for a weekend:

1. **"Explain this repo" (streaming).** A card on `ResultDetailScreen` streams a plain-English
   summary of a repository's README, token by token, with Stop and Retry.
2. **Natural-language search (structured output, i.e. generative UI).** Typing
   *"kotlin image loading libs with 1k+ stars"* into the bar returns a typed `SearchIntent` that
   becomes GitHub search qualifiers (`language:kotlin stars:>1000 image loading`), shown as
   editable filter chips before the search runs.

Total time is about 12–14 hours. Each block lists **Learn** (concepts to read up on), **Build**
(the code) and **Done when** (a checkpoint you can verify).

---

## Architecture at a glance

```
Android app (:feature:detail, :shared)          Tiny proxy (:proxy, Ktor server, JVM)
┌───────────────────────────────┐   SSE    ┌──────────────────────────────────────┐
│ ExplainRepoViewModel          │ <─────── │ GET /explain?repo=owner/name         │
│  StateFlow<ExplainUiState>    │          │  1. fetch README from GitHub         │
│ LlmClient (Ktor client + SSE) │ ───────> │  2. Anthropic Java SDK, streaming    │
└───────────────────────────────┘   HTTP   │  3. re-emit text deltas as SSE       │
                                           │ POST /parse-search → SearchIntent    │
                                           └──────────────────────────────────────┘
```

**Why a proxy?** An API key inside an APK can be extracted, and once that happens anyone can run
up your bill. Production apps call their own backend, which holds the key, applies rate limits,
and can swap models without an app release. For the weekend the proxy runs on your laptop; the
emulator reaches it at `http://10.0.2.2:8080`. You will need a debug-only
`network_security_config` that allows cleartext to `10.0.2.2`.

The client half (`LlmClient`) can live in `:shared/commonMain`. Ktor's SSE plugin is
multiplatform, so the iOS app gets the feature almost for free, which is a strong point to raise
in a KMP interview.

---

## 1. Friday evening: setup (1.5 h)

**Learn**
- How LLM APIs work: a messages list in, content blocks out. Tokens, context window, `max_tokens`,
  and `stop_reason` (`end_turn`, `max_tokens`, `refusal`).
- Server-Sent Events: a long-lived HTTP response made of `data: ...\n\n` frames. It's why chat UIs
  feel fast, because the first token can arrive in under a second even when the full answer takes
  ten.

**Build**
1. Get an Anthropic API key (console.anthropic.com) and `export ANTHROPIC_API_KEY=...` in the
   shell that runs the proxy. Never put it in `local.properties` or `BuildConfig`.
2. Add a `:proxy` JVM module (Ktor server, `ktor-server-netty`, `ktor-server-sse`) plus
   `com.anthropic:anthropic-java`. Verify the latest stable versions at implementation time.
3. Write a hello-world streaming endpoint (below) and test it with
   `curl -N "localhost:8080/explain?repo=square/okhttp"`.

```kotlin
// proxy/src/main/kotlin/Application.kt
fun main() {
    val claude = AnthropicOkHttpClient.fromEnv() // reads ANTHROPIC_API_KEY
    val http = HttpClient(CIO)

    embeddedServer(Netty, port = 8080) {
        install(SSE)
        routing {
            sse("/explain") {
                val repo = call.parameters["repo"] ?: return@sse close()
                val readme = http
                    .get("https://raw.githubusercontent.com/$repo/HEAD/README.md")
                    .bodyAsText()
                    .take(MAX_README_CHARS) // keep cost predictable; tell the user if truncated

                val params = MessageCreateParams.builder()
                    .model("claude-opus-5-5")
                    .maxTokens(4_000L)
                    .system(EXPLAIN_SYSTEM_PROMPT)
                    .addUserMessage("<readme>\n$readme\n</readme>")
                    .build()

                claudeEvents(claude, params).collect { send(it) }
            }
        }
    }.start(wait = true)
}

// The SDK stream is blocking: read it in a flow on IO, then send() from the SSE session.
// flowOn keeps event order, and cancelling the collector (client disconnected) closes the
// SDK stream through use{}, so you stop paying for tokens nobody will read.
private fun claudeEvents(claude: AnthropicClient, params: MessageCreateParams) = flow {
    claude.messages().createStreaming(params).use { response ->
        for (event in response.stream().iterator()) {
            event.contentBlockDelta().flatMap { it.delta().text() }.orElse(null)
                ?.let { emit(ServerSentEvent(data = it.text())) }
            // e.g. "end_turn" or "refusal": let the client tell them apart
            event.messageDelta().flatMap { it.delta().stopReason() }.orElse(null)
                ?.let { emit(ServerSentEvent(event = "stop", data = it.toString())) }
        }
    }
}.flowOn(Dispatchers.IO)

private const val MAX_README_CHARS = 40_000
private val EXPLAIN_SYSTEM_PROMPT = """
    You explain GitHub repositories to Android developers. Given a README, write:
    one sentence on what it is, then 3 short bullets on when to use it, then one line
    on alternatives. Plain Markdown, no headings, under 120 words.
""".trimIndent()
```

> `stopReason()` returns an SDK enum wrapper, and its `toString()` gives the raw value
> (`refusal`, `end_turn`). If the compiler disagrees with an accessor name, follow its error;
> the SDK's builder and accessor names are the only part of this snippet that can drift.

**As built in `:proxy`**, the sketch above changes in a few places:
- The README comes from GitHub's `GET /repos/{repo}/readme` (raw media type), which finds
  `README.rst` or `readme.md` too; `raw.githubusercontent.com/.../README.md` misses those.
- `repo` is validated as `owner/name` before it goes into a URL.
- Every failure becomes one `event: error` frame (`invalid_repo`, `readme_not_found`,
  `upstream_error`), and a cut README sends one `event: truncated` frame. Once an SSE response
  has started, its HTTP status can't carry the error.
- The request sets `effort: low`, because Claude Opus 5.5 always thinks, and low effort keeps
  time to first token short for a simple summary.
- README fetching and the Claude call sit behind interfaces, so route tests use fakes, with no
  key, network or cost.

**Done when:** `curl -N` prints text arriving in chunks rather than all at once.

---

## 2. Saturday morning: streaming UI (3 h)

**Learn**
- Modelling non-deterministic output as UI state. A response can be idle, waiting for the first
  token, streaming, complete, stopped by the user, refused, or failed partway. Each one needs its
  own UI.
- `callbackFlow`/`flow` from Ktor's `client.sse { }`, and cancellation: cancelling the collecting
  coroutine closes the HTTP connection, and that *is* your Stop button.

**Build**: the client (`:shared/commonMain`)

```kotlin
sealed interface LlmChunk {
    data class Text(val value: String) : LlmChunk
    data class Finished(val stopReason: String) : LlmChunk
}

class LlmClient(private val http: HttpClient, private val baseUrl: String) {
    // http must have install(SSE)
    fun explainRepo(fullName: String): Flow<LlmChunk> = flow {
        http.sse("$baseUrl/explain", { parameter("repo", fullName) }) {
            incoming.collect { event ->
                when (event.event) {
                    "stop" -> emit(LlmChunk.Finished(event.data.orEmpty()))
                    else -> event.data?.let { emit(LlmChunk.Text(it)) }
                }
            }
        }
    }
}
```

**Build**: state and the ViewModel (`:feature:detail`)

```kotlin
sealed interface ExplainUiState {
    data object Idle : ExplainUiState
    data object WaitingForFirstToken : ExplainUiState
    data class Streaming(val text: String) : ExplainUiState
    data class Done(val text: String, val stoppedByUser: Boolean = false) : ExplainUiState
    data class Refused(val partial: String) : ExplainUiState
    data class Failed(val partial: String, val message: String) : ExplainUiState
}

@HiltViewModel
class ExplainRepoViewModel @Inject constructor(
    private val llm: LlmClient,
) : ViewModel() {
    private val _state = MutableStateFlow<ExplainUiState>(ExplainUiState.Idle)
    val state: StateFlow<ExplainUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun explain(fullName: String) {
        job?.cancel()
        _state.value = ExplainUiState.WaitingForFirstToken
        job = viewModelScope.launch {
            val text = StringBuilder()
            try {
                llm.explainRepo(fullName).collect { chunk ->
                    when (chunk) {
                        is LlmChunk.Text -> {
                            text.append(chunk.value)
                            _state.value = ExplainUiState.Streaming(text.toString())
                        }
                        is LlmChunk.Finished -> _state.value =
                            if (chunk.stopReason == "refusal") ExplainUiState.Refused(text.toString())
                            else ExplainUiState.Done(text.toString())
                    }
                }
            } catch (e: CancellationException) {
                // Only a Stop leaves this job current. A Retry has already replaced `job` and
                // set WaitingForFirstToken, which this late catch must not overwrite.
                if (job == coroutineContext.job) {
                    _state.value = ExplainUiState.Done(text.toString(), stoppedByUser = true)
                }
                throw e
            } catch (e: Exception) {
                // keep partial text, because losing half an answer is worse UX than showing it
                _state.value = ExplainUiState.Failed(text.toString(), e.message ?: "Network error")
            }
        }
    }

    fun stop() { job?.cancel() }
}
```

In the composable, render each state: a shimmer for `WaitingForFirstToken`, text plus a blinking
caret and a **Stop** button for `Streaming`, and the partial text plus a **Retry** button for
`Failed`.

**Done when:** tapping "Explain" on the OkHttp detail page streams text in. Stop freezes it. Turning
on airplane mode partway shows the partial text with Retry.

---

## 3. Saturday afternoon: UX polish and tests (3 h)

This is where the work looks senior instead of like a demo. Pick at least four:

| Concern | What to do |
|---|---|
| **Jank** | Tokens can arrive 50+ times a second. Batch UI updates (e.g. `sample(50.milliseconds)` on the state, or append in ~16 ms frames) and measure recompositions with Layout Inspector. |
| **Markdown** | Render bullets and `code` incrementally. Be careful with half-received syntax (`**bo`), which a Markdown renderer must not choke on. |
| **Scrolling** | Auto-scroll while streaming, but stop following as soon as the user scrolls up. Show a "↓ new text" pill. |
| **Accessibility** | Don't make TalkBack read every token. Announce "Summary ready" on `Done` (`liveRegion = Polite` on a status node, not on the text). |
| **Trust** | "AI-generated, may be inaccurate" label, plus 👍/👎 feedback. Never present output as GitHub's own data. |
| **Cost/latency** | Cache summaries per `fullName` (the existing SQLDelight cache pattern fits; see `docs/architecture/github-search-caching-decision.md`). A second visit should be instant and free. |
| **Process death** | Final text survives in `SavedStateHandle` (or the cache). Don't try to resume a stream after process death; just offer Retry. |

**Tests (Learn: test the UI against a fake, not the model):**

```kotlin
class FakeLlmClient(private val chunks: List<LlmChunk>, private val failAfter: Int? = null) {
    fun explainRepo(fullName: String) = flow {
        chunks.forEachIndexed { i, c ->
            if (i == failAfter) throw IOException("boom")
            emit(c)
        }
    }
}

@Test fun `network error mid-stream keeps partial text`() = runTest {
    val vm = ExplainRepoViewModel(fake(listOf(Text("Ok"), Text("Http")), failAfter = 2))
    vm.state.test {
        vm.explain("square/okhttp")
        // Idle → Waiting → Streaming("Ok") → Streaming("OkHttp") → Failed("OkHttp", ...)
        assertThat(expectMostRecentItem()).isEqualTo(Failed("OkHttp", "boom"))
    }
}
```

To inject a fake you'll need an interface over `LlmClient`, which is the same segregation argument
as `GitHubSearchNetworkSource`. Add Paparazzi snapshots for each `ExplainUiState`; the repo already
uses Paparazzi.

**Done when:** each state has a unit test and a snapshot, and you can explain the jank fix you
measured.

---

## 4. Sunday morning: structured output, i.e. generative UI (3 h)

**Learn**
- **Structured outputs:** constrain the model to a JSON schema so the app receives a *type* rather
  than prose. This is the core idea behind generative UI: the model picks *what* to show, and your
  native components decide *how*.
- **Validate anyway.** The schema guarantees shape, not sense (`minStars = -5`, an invented
  language). Clamp and sanitise before the result touches your query builder.
- Prompt injection: whatever the user types is data, never instructions. Your proxy must only ever
  return a `SearchIntent`; it must not call tools based on free text.

**Build**: the proxy endpoint

```kotlin
// Shared shape. Jackson derives the JSON schema from it (add jackson-module-kotlin, or use
// a Java record if Kotlin data class schema derivation gives you trouble; verify at
// implementation time).
data class SearchIntent(
    val keywords: String,
    val language: String?,
    val minStars: Int?,
    val type: String, // "repositories" | "users" | "both"
)

post("/parse-search") {
    val query = call.receiveText().take(300)
    val params = MessageCreateParams.builder()
        .model("claude-opus-5-5")
        .maxTokens(1_000L)
        .system("Convert a GitHub search request into filters. Leave a field null if not mentioned.")
        .outputConfig(SearchIntent::class.java)
        .addUserMessage(query)
        .build()

    val intent = withContext(Dispatchers.IO) {
        claude.messages().create(params).content()
            .firstNotNullOfOrNull { it.text().orElse(null)?.text() }
    }
    if (intent == null) call.respond(HttpStatusCode.BadGateway) else call.respond(intent)
}
```

**Build**: the app side
- `SearchIntent.toGitHubQuery()`: `"${keywords} language:$language stars:>$minStars"`, with each
  part skipped when null. Unit-test this pure function thoroughly.
- In the autocomplete bar, a ✨ toggle switches to "describe what you want" mode. On submit, show
  the parsed intent as **editable `FilterChip`s** (`Kotlin ✕`, `≥ 1k ★ ✕`) above the results, so
  the user can fix the model's interpretation in one tap instead of retyping.
- Fallback: if parsing fails or times out (set a 3 s budget), run the raw text as a normal search.
  The feature must never make search *worse*.

**Done when:** *"popular kotlin image loading libraries"* produces the chips `Kotlin` and
`image loading`, plus a sensible star filter, and the results match. Removing a chip re-runs the
search.

---

## 5. Sunday afternoon: on-device option (2 h)

**Learn**
- **Gemini Nano via ML Kit GenAI APIs** (running in AICore) handles summarise, proofread, rewrite
  and describe-image on supported devices. Benefits: offline, private, and no per-call cost.
  Limits: device support, a smaller model, and a one-time model download.
- The **hybrid routing** pattern: check the on-device feature's status first, use it if available,
  otherwise fall back to the cloud. Interviewers like this tradeoff discussion.

**Build**: the same "Explain" card, routed

```kotlin
interface RepoExplainer { fun explain(item: RepoResult, readme: String): Flow<LlmChunk> }

class OnDeviceExplainer(/* ML Kit Summarizer, OutputType.THREE_BULLETS */) : RepoExplainer
class CloudExplainer(private val llm: LlmClient) : RepoExplainer

class RoutingExplainer(
    private val onDevice: OnDeviceExplainer,
    private val cloud: CloudExplainer,
) : RepoExplainer {
    // checkFeatureStatus(): AVAILABLE → on-device; DOWNLOADABLE → start download, use cloud
    // this time; UNAVAILABLE → cloud. Expose which path ran so the UI can say "On-device ✓".
}
```

Check the current ML Kit GenAI Summarization docs for the exact artifact and API names; this area
moves quickly. If your test device isn't supported, still write the routing layer with a fake
`OnDeviceExplainer`. The architecture is what you'll be discussing.

**Done when:** the card shows which engine produced the summary, and turning off Wi-Fi on a
supported device still works.

---

## 6. Sunday evening: write it up (1 h)

1. Add a short "AI features" section to the README with a GIF of streaming and the chips.
2. Record the decisions in `docs/architecture/` (proxy vs direct, partial-text-on-error, chips
   for correction, on-device routing) in the same style as the caching decision doc.
3. Prepare your interview story (below).

---

## What to say in an interview

> "In my GitHub search app I added two LLM features. First, a streaming repo explainer: a Ktor
> SSE client in the KMP shared module, a sealed UI state covering waiting, streaming, stopped,
> refused and failed-with-partial-text, and Stop implemented as coroutine cancellation that closes
> the connection. I throttled updates to avoid recomposition jank and announced completion to
> TalkBack rather than every token. Second, natural-language search using structured output: the
> model returns a typed `SearchIntent` that I validate and show as editable filter chips, so
> users can correct the model in one tap, with fallback to plain search. The key lives in a small
> backend proxy, never in the APK, and summaries go through on-device Gemini Nano when the device
> supports it."

**Possible CV bullet:**
*Built LLM-powered features in a Kotlin Multiplatform/Compose app: token-streaming UI over SSE with
cancellation and partial-failure states, structured-output "generative UI" mapping model responses
to native filter chips, and hybrid on-device (Gemini Nano) / cloud inference behind a key-holding
backend proxy.*

## Self-check questions

You know the material when you can answer these without notes:

1. Why does streaming improve perceived latency but not total latency? Which metric do you measure
   (time to first token)?
2. What happens to the HTTP connection when the user leaves the screen mid-stream? Where is that
   handled in your code?
3. Why keep the partial text on failure rather than show an error screen?
4. Structured output guarantees the schema. What does it *not* guarantee, and where do you
   validate?
5. Name two ways an API key in an APK leaks, and two things the proxy gives you besides hiding the
   key.
6. When would you choose on-device over cloud, and how do you handle a device that doesn't
   support it?
7. How do you UI-test a non-deterministic feature?
