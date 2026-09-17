# Ticket: `GitHubApi` never checks HTTP status

**Status:** fixed and merged (PR #5). **Origin:**
`docs/backlog.md` AT-11, found during a source-code
bug-hunting pass and verified against the actual code before being written up. This file exists so
the original problem record survives in git history even if the backlog entry is later edited or
removed once fixed — "keep the ticket," not just the fix.

---

## Problem

`GitHubApi.kt`'s `searchRepositories`/`searchUsers` call `.body()` directly on the Ktor response,
with no HTTP status check anywhere:

```kotlin
suspend fun searchRepositories(query: String, perPage: Int): GitHubSearchResponse<RepositoryDto> =
    client.get("$GITHUB_BASE_URL/search/repositories") {
        parameter("q", query)
        parameter("per_page", perPage)
    }.body()
```

Ktor 3.5.2 defaults `expectSuccess = false`, and nothing in this codebase overrides it (confirmed
via a repo-wide search for `expectSuccess`/`HttpResponseValidator` — no matches). That means
`.body()` runs against **every** HTTP status code, not just 2xx.

GitHub's real error response for a rate-limited or invalid search is a small JSON object with a
totally different shape than a successful search response:

```json
{"message": "API rate limit exceeded for ...", "documentation_url": "https://docs.github.com/..."}
```

`GitHubSearchResponse<T>` requires three non-nullable, no-default fields — `total_count`,
`incomplete_results`, `items` — none of which exist on that error body. kotlinx.serialization
throws `MissingFieldException` trying to deserialize it, with a message like:

> `Field 'total_count' is required, but it was missing`

That raw, misleading message is what ends up in front of the user — it propagates unchanged
through `GitHubSearchNetworkSourceImpl` → `CachingGitHubSearchRepository` →
`SearchAutocompleteUseCase`'s `.catch { e -> emit(SearchOutcome.Failure(e.message ...)) }` →
`AutocompleteUiState.Error(message)` on Android, and the equivalent path on iOS. A real, expected
occurrence (GitHub's search API rate-limits unauthenticated requests — see the README's "Known
limitations") reads to the user as a broken JSON parser, not "you're rate limited, try again."

**Why `GitHubApiTest.kt` never caught this:** every fixture in that file constructs
`HttpStatusCode.OK` responses. No test ever builds a non-2xx response, so this path has zero test
coverage today.

## Decision

Add a small `GitHubApiException` sealed hierarchy (`shared/.../remote/GitHubApiException.kt`) and
check `HttpResponse.status` before attempting to deserialize a body, mapping non-2xx responses to
a specific, human-readable case instead of letting deserialization fail with an unrelated error:

- `RateLimited` — 403 or 429 (GitHub's search API returns 403 for its primary rate limit; 429 is
  the secondary-rate-limit status GitHub's docs describe for other endpoints — both paths are
  covered by a test, see Verification).
- `Unauthorized` — 401 (this client sends no `Authorization` header today, so this shouldn't occur
  in practice, but it's a real GitHub API status worth naming rather than falling into a generic
  bucket).
- `NotFound` — 404.
- `ServerError` — any 5xx.
- `Unknown` — anything else non-2xx, carrying the raw status code and body so a genuinely
  unanticipated response is still debuggable instead of silently swallowed into a generic message.

`GitHubSearchRepository.search()` already carries `@Throws(Exception::class)` for the iOS bridge —
since `GitHubApiException` is an `Exception` subtype, no change is needed there; the existing
annotation already covers it. Swift will still see a generic `NSError` rather than being able to
pattern-match on the specific case (a harder interop problem than this fix takes on — see
`docs/kmp-knowledge/kmp-knowledge-base.md` §13 for that as a distinct, deferred idea). What
*does* change for both platforms: the exception's `.message` is now accurate instead of a raw
serialization error, since the whole chain propagates `.message` as a plain `String` already.

## What changed

- **New:** `GitHubApiException.kt` (`shared/.../remote/`) — the sealed hierarchy described above.
- **`GitHubApi.kt`:** `searchRepositories`/`searchUsers` now call a new private
  `HttpResponse.bodyOrThrow<T>()` extension instead of `.body()` directly. It checks
  `status.isSuccess()` first; on failure it reads the raw body (best-effort, via `runCatching` —
  a body read can itself fail) and maps the status code to a `GitHubApiException` case: 401 →
  `Unauthorized`, 403/429 → `RateLimited`, 404 → `NotFound`, 5xx → `ServerError`, anything else →
  `Unknown(statusCode, rawBody)`.
- **`GitHubApiTest.kt`:** nine cases total covering this fix. The first pass (3 tests) only
  covered the two status codes named in the original bug report (403, 422) plus 500. A follow-up
  pass closed the gaps that left: 404 → `NotFound`, 401 → `Unauthorized`, and 429 → `RateLimited`
  (proving the "403 and 429 both mean rate-limited" decision above actually holds, not just 403)
  each got their own test, since nothing had exercised those `when` branches at all; plus one test
  for a non-JSON (HTML) error body on a 503, since a real outage can return plain text instead of
  GitHub's normal JSON shape, and `bodyOrThrow()` deliberately reads the error body as plain text
  rather than JSON specifically so that doesn't crash a second time on top of the original error.
- **`CachingGitHubSearchRepositoryTest.kt` and `SearchAutocompleteUseCaseTest.kt`:** both already
  had failure-path tests, but only using a generic `RuntimeException` to stand in for "some
  failure." One test added to each, using the real `GitHubApiException` type, proving the
  cache-fallback path and the use-case's failure-message propagation both work with this specific
  type too — not just with exceptions in general.
- **No changes needed** to `GitHubSearchRepository.kt`'s `@Throws(Exception::class)`,
  `CachingGitHubSearchRepository`'s catch-and-fall-back-to-cache logic, or
  `SearchAutocompleteUseCase`'s `.catch { e -> emit(SearchOutcome.Failure(e.message ...)) }` — all
  three already operate on `Exception`/`.message` generically, so the new exception types flow
  through the existing chain automatically. A rate-limit hit now shows "GitHub API rate limit
  exceeded (HTTP 403) - try again shortly" instead of "Field 'total_count' is required" on both
  Android and iOS, without either platform's UI code changing.

## Verification

- `./gradlew :shared:testAndroidHostTest --tests "*GitHubApiTest*" --tests
  "*CachingGitHubSearchRepositoryTest*" --tests "*SearchAutocompleteUseCaseTest*"` —
  `GitHubApiTest` 9/9, `CachingGitHubSearchRepositoryTest` 4/4, `SearchAutocompleteUseCaseTest`
  7/7, all pass.
- `./gradlew :shared:iosSimulatorArm64Test` (full `commonTest` suite, not just this fix's tests)
  — passes on the Kotlin/Native target too.
- `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest` — compiles clean, full existing
  suite still green (no regression from `GitHubApiException` now being a possible thrown type
  anywhere `GitHubSearchRepository.search()` is called).
- **Not verified live against the real GitHub API** — this was fixed and tested via `MockEngine`
  fixtures constructing the exact status codes/bodies GitHub's docs describe, not by triggering an
  actual rate limit. Worth a real-device check next time a rate limit is hit organically, same
  spirit as how the `ignoreUnknownKeys` gap was originally found (live, not by guessing).
- **Deliberately not attempted:** Swift-side pattern matching on the specific `GitHubApiException`
  case via the bridged `NSError`. iOS still sees a generic `NSError` with the correct `message` —
  which already fixes the actual bug (a clear message instead of a parse-failure message) — but
  doesn't let `ContentView.swift` branch on "was this a rate limit specifically." Left as the
  separate, harder extension idea already tracked in
  `docs/kmp-knowledge/kmp-knowledge-base.md` §13.
