# :proxy — the LLM backend

A small Kotlin server that sits between the Android app and Claude. The app asks it to
"explain this repo"; the proxy fetches the repo's README from GitHub, asks Claude for a short
summary, and streams the answer back word by word.

**Why a server at all?** Calling Claude needs an API key. Any key shipped inside an APK can be
extracted, and then anyone can spend your credits. The key lives only here, in an environment
variable on the machine running the proxy. A real backend would also be where you add rate
limits, caching, or a model switch, without an app release.

It's a plain JVM app, not Android: nothing in `:app`, `:feature:*` or `:shared` depends on it at
build time. The app will reach it over HTTP (step 2 of `docs/learning/llm-powered-ui-weekend.md`).

## What kind of backend this is

It's a backend: a separate program on a server (for now, your laptop) that the app calls over
HTTP, and the only place holding a secret. It's a thin one, though, often called a *proxy* or
*BFF (backend for frontend)*. It checks input, forwards to GitHub and Claude, and reshapes the
answer into SSE frames. It keeps no state: every request stands alone, and a restart between
two requests loses nothing.

Compared with the backend a real product (say, a shop) needs:

| | `:proxy` today | Full backend, e.g. a shop |
|---|---|---|
| Database | none | e.g. Postgres: products, carts, orders |
| Users / auth | none, open to anyone who can reach it | accounts, login (e.g. JWT) |
| Business rules | none, pass-through | stock checks, pricing, checkout |
| Deployment | `./gradlew :proxy:run` on your laptop | hosted server with HTTPS |
| Stack | Ktor server, Kotlin, JVM | the same |

What carries over: routing, streaming, error handling, secrets in environment variables, and
testing routes with `testApplication`. What's still new: database, auth, deployment.

**Before running it anywhere but your laptop:** it has no auth, so anyone who can reach it can
spend your Claude credits. Today two guards keep that small: it listens on `127.0.0.1` only (other
machines on your Wi-Fi can't connect) and allows 10 `/explain` calls a minute. A deployed
version would also need a shared secret or real auth.

## Folder map

```
proxy/
├── build.gradle.kts         what this module is: Kotlin JVM + `application` (gives ./gradlew :proxy:run)
├── src/main/kotlin/pl/gi/codingchallenge/proxy/
│   ├── Application.kt       entry point: checks the key, builds real clients, starts the server
│   ├── ExplainRoutes.kt     the HTTP part: GET /explain, validation, errors, SSE frames
│   ├── ReadmeSource.kt      gets README text from GitHub
│   └── RepoExplainer.kt     the Claude part: prompt, model settings, streaming the reply
├── src/main/resources/
│   └── logback.xml          logging format and levels (Ktor logs through SLF4J → Logback)
└── src/test/kotlin/pl/gi/codingchallenge/proxy/
    └── ExplainRoutesTest.kt route tests with fake GitHub + fake Claude
```

## What each file does

**`Application.kt`** — the *composition root*: the one place that creates real objects and
wires them together. It refuses to start without `ANTHROPIC_API_KEY` (failing on startup is
easier to spot than failing on the first request), reads `PORT` (default 8080), creates one
shared GitHub HTTP client and one Claude client, and starts a Netty server on `127.0.0.1`
(this machine only). When the server stops, it closes both clients.

**`ExplainRoutes.kt`** — defines `GET /explain?repo=owner/name` inside
`Application.proxyModule(...)`. It knows about HTTP and SSE, but not about Claude or GitHub
details: it gets a `ReadmeSource` and a `RepoExplainer` passed in. Its job, in order:
1. validate `repo` against `owner/name` (also stops anything odd reaching a GitHub URL),
2. fetch the README,
3. cut it to 40,000 characters if longer, and say so (keeps cost per call predictable),
4. turn each `ExplainEvent` into an SSE frame,
5. turn any failure into one `error` frame.

**`ReadmeSource.kt`** — an interface (`fetch(repo): String?`) plus `GitHubReadmeSource`, the
real one. It calls GitHub's `/repos/{repo}/readme` endpoint, which finds the README whatever
it's called (`README.md`, `readme.rst`…). The `raw` media type makes GitHub return the file
itself instead of JSON with base64 inside. 404 → `null` ("no README"); other errors throw.

**`RepoExplainer.kt`** — an interface (`explain(readme): Flow<ExplainEvent>`) plus
`ClaudeRepoExplainer`, the real one. It builds the request (model, system prompt, effort,
token limit), opens a streaming call through the Anthropic Java SDK, and emits `Text` for each
chunk of the reply and `Stop` with the reason at the end. `ExplainEvent` is the proxy's own
small type, so the route doesn't depend on SDK classes.

**`ExplainRoutesTest.kt`** — starts the route in Ktor's in-memory `testApplication` (no real
port) with fakes for both interfaces, then checks the frames that come back. No key, no
network, no cost, so it runs in `./gradlew check` and CI.

## One request, start to finish

```
curl / app            ExplainRoutes             GitHubReadmeSource      ClaudeRepoExplainer
    │  GET /explain?repo=square/okhttp │                      │                      │
    │─────────────────────────────────>│ validate repo        │                      │
    │                                  │─────fetch──────────> │ GET api.github.com   │
    │                                  │<────README text───── │                      │
    │                                  │ cut to 40k chars     │                      │
    │                                  │──────────────explain(readme)───────────────>│ streaming
    │  data: OkHttp is…                │<────────────Text("OkHttp is…")──────────────│ call to
    │<─────────────────────────────────│<────────────Text(" an HTTP…")───────────────│ Claude
    │  data:  an HTTP…                 │                 …                           │
    │  event: stop / data: end_turn    │<────────────Stop("end_turn")────────────────│
    │<─────────────────────────────────│ handler returns → stream closes             │
```

## What the client receives

| Frame | When | `data` |
|---|---|---|
| *(no event name)* | each chunk of the summary | the text chunk |
| `stop` | once, at the end | `end_turn` (finished), `max_tokens` (cut off), or `refusal` (model declined) |
| `truncated` | once, before the text, only if the README was cut | a short note |
| `error` | once, instead of the rest | `invalid_repo`, `readme_not_found`, or `upstream_error` |

If Claude fails halfway, the text already sent stays sent, and an `error` frame follows.

The one exception: past 10 calls a minute, the request is refused **before** the stream starts,
so it gets a normal `429 Too Many Requests` with a `Retry-After` header and no frames.

## Concepts worth knowing

**SSE (Server-Sent Events).** A normal HTTP response that stays open and sends small
`data: …` blocks as they're ready. That's why chat UIs feel fast: the first words arrive in
about a second even if the full answer takes ten. Test it with `curl -N` (`-N` turns off
curl's buffering).

**Why errors are frames, not status codes.** An SSE response sends `200 OK` and its headers
before the first frame. After that the status can't change, so an `error` frame is the only
way to report a problem, even for a bad `repo` parameter.

**Blocking SDK, coroutine server.** The Anthropic Java SDK reads the stream by blocking a
thread. Ktor runs on a few shared threads that must never block. `flowOn(Dispatchers.IO)`
moves the reading to the IO thread pool, which is meant for blocking work.

**Cancellation saves money.** If the client disconnects, Ktor cancels the route's coroutine
(at the latest when sending the next frame fails).
The flow stops at its next `emit`, and `use { }` closes the SDK stream, which ends the request
to Claude, so you stop paying for tokens nobody will read. This is also how the app's Stop
button will work. `CancellationException` is re-thrown in the route rather than reported as
`upstream_error`, because it's a normal stop, not a failure.

**Interfaces for testability.** `ReadmeSource` and `RepoExplainer` exist so tests can pass
lambdas instead of real GitHub and Claude clients (`fun interface` makes that a one-liner).
Same idea as `GitHubSearchNetworkSource` in `:shared`.

**Model settings** (in `ClaudeRepoExplainer`):
- `claude-opus-5-5` with `effort: low`. Opus 5.5 always does some thinking before it answers;
  low effort keeps that short, so the first words arrive sooner. A summary doesn't need more.
- `fallbacks: "default"` (a beta feature): if the model declines a request, the API retries on
  another model picked by the reason, instead of just stopping.
- `max_tokens` 4,000 is a ceiling, not a target; the prompt asks for under 120 words.
- The README goes inside `<readme>…</readme>` tags so the model treats it as material to
  summarise, not as instructions.

**Usage guards.** Three layers stop accidental spending, from strongest to weakest:
1. *Prepaid credits with auto-reload off* (set in the Anthropic console): a hard cap no bug can
   pass. A monthly spend limit there is a second check.
2. *Per-call caps* in code: README cut at 40k characters, reply capped at 4,000 tokens, so one
   call costs about $0.12 at most (about $0.05 typically).
3. *Rate limit*: Ktor's `RateLimit` plugin allows 10 `/explain` calls a minute, which catches an
   app bug such as a retry loop. It's a token bucket: 10 tokens, refilled every minute.

**Version catalog.** Ktor server artifacts reuse the catalog's `ktor` version, the same one
`:shared`'s client uses, so the two never drift apart.

## Running and testing

```
export ANTHROPIC_API_KEY=sk-ant-...      # never in the repo or local.properties
./gradlew :proxy:run                     # starts on :8080, keeps running
curl -N "localhost:8080/explain?repo=square/okhttp"
./gradlew :proxy:test                    # route tests, no key needed
```

Each real `/explain` call costs a few cents.

## Where this is heading

The single flat package is deliberate while there's one endpoint. When `/parse-search`
arrives (step 4 of the weekend plan), group by feature, the same way the app has
`:feature:*` modules:

```
proxy/
├── Application.kt
├── explain/   ExplainRoutes · ReadmeSource · RepoExplainer
└── search/    ParseSearchRoutes · SearchIntent
```

The Claude client stays created once in `Application.kt` and is passed to both.
