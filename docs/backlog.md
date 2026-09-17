# Awaiting Tasks

Known gaps, rough edges, and stale documentation noticed during work on this repo, deliberately
**not** fixed at the time they were found — either out of scope for the task in progress, or not
yet judged worth the effort. Revisit each ticket and decide then whether it's worth solving; this
file is a record of "noticed and deferred," not a commitment to fix.

Each ticket: priority, component, what's wrong, why it wasn't fixed immediately, and what fixing
it would actually involve.

---

## Open

### AT-1 — `SuggestionPanel`'s LazyColumn key can collide across result types (crash)

| | |
|---|---|
| **Priority** | High |
| **Component** | `:app` — `ui/autocomplete/SuggestionPanel.kt` |

**Problem:** `SuggestionPanel` merges users and repos into one list and keys each `itemsIndexed`
row on `item.id` alone (`key = { _, item -> item.id }`). `RepositoryDto.id` and `UserDto.id` are
both plain `Long`s from GitHub's REST API, but users and repos are independent entity types with
their own ID sequences — a user and a repository can legitimately share the same numeric ID. Any
search whose merged, top-50 result set contains such a pair throws `IllegalArgumentException: Key
... was already used` from Compose's `LazyColumn`, crashing the screen. Nothing in the current
fixtures/tests uses colliding IDs across the two types, so this hasn't surfaced in
`GitHubAutocompleteBarTest` or the Paparazzi goldens.

**Why deferred:** found during a codebase-wide bug sweep, not hit organically yet — needs a live
query that happens to return a colliding pair to reproduce, which is a matter of when, not if,
against real GitHub data.

**Fix:** disambiguate the key by result type, e.g.
`key = { _, item -> "${item::class.simpleName}:${item.id}" }`, or add a type discriminator to
`SearchResultItem.id` itself so downstream consumers don't have to remember the collision risk.

**Cross-platform note:** `iosApp/iosApp/ContentView.swift:71` keys SwiftUI's `List` on the same
`item.id` — identical root cause. SwiftUI's `List` doesn't hard-crash on a duplicate identifier the
way Compose's `LazyColumn` does, but produces undefined diffing/identity behavior instead (a known
SwiftUI foot-gun) — not run live to confirm the exact symptom, so treat as plausible-but-unconfirmed
rather than a second verified crash. Worth applying the same fix on both platforms together.

### AT-2 — `CachingGitHubSearchRepository` can swallow `CancellationException`

| | |
|---|---|
| **Priority** | Medium |
| **Component** | `:shared` — `domain/repository/CachingGitHubSearchRepository.kt` |

**Problem:** `CachingGitHubSearchRepository.search()` wraps the network call in
`try { ... } catch (e: Exception) { cache.get(query) ?: throw e }`. `kotlinx.coroutines
.CancellationException` is a subtype of `Exception`, so it's caught here too. This path is live in
normal use: `SearchAutocompleteUseCase` runs searches through `flatMapLatest`, which cancels the
in-flight search for the previous keystroke as soon as a new one arrives. A search cancelled
mid-flight gets treated as an ordinary failure and falls through to a cache lookup for the
now-stale query, instead of propagating the cancellation — a well-known coroutines correctness
pitfall (swallowing `CancellationException` breaks structured concurrency). No test currently
exercises this path; `CachingGitHubSearchRepositoryTest` only covers `RuntimeException`.

**Why deferred:** found during a codebase-wide bug sweep. Low visible impact today (the merged
`flatMapLatest` flow already discards the stale coroutine's result either way), but it's a latent
correctness issue that would bite harder if this fallback logic is ever reused somewhere the
result of a swallowed cancellation is actually observed.

**Fix:** re-throw cancellation before the generic catch, e.g.
`catch (e: CancellationException) { throw e } catch (e: Exception) { cache.get(query) ?: throw e }`,
plus a test asserting a cancelled `search()` propagates cancellation instead of returning a cached
value.

### AT-3 — SQLDelight search cache has no eviction or size cap

| | |
|---|---|
| **Priority** | Low |
| **Component** | `:shared` — `cache/SearchResultCache.kt`, `CachedSearch.sq` |

**Problem:** `cachedSearch` (`CachedSearch.sq`) upserts one row per distinct query string with no
TTL, max-row-count, or LRU eviction. Since `SearchAutocompleteUseCase` debounces per keystroke
past 3 characters, typing one word can write several rows (`"kot"`, `"kotl"`, `"kotli"`, ...), all
kept forever. Over normal usage the table grows unbounded on-device storage with no cleanup path.

**Why deferred:** found during a codebase-wide bug sweep; not a crash or correctness bug, and the
README's "Known limitations" already flags the cache as non-general-purpose — this is a related
but distinct gap (unbounded growth, not staleness).

**Fix:** either a row cap with LRU eviction (e.g. delete oldest beyond N rows on `upsert`), a TTL
column checked on read, or deliberately deciding unbounded growth is acceptable for this cache's
realistic lifetime and documenting that choice.

### AT-4 — README architecture section describes packages that no longer exist

| | |
|---|---|
| **Priority** | High |
| **Component** | Docs — `README.md` |

**Problem:** the README's architecture/module-tree section shows `domain/` and `data/` living
under `pl.gi.codingchallenge` (`:app`), with named files like `di/NetworkModule.kt`,
`data/remote/GitHubApi.kt # Retrofit interface`, and `data/repository/GitHubSearchRepositoryImpl.kt`.
Re-verified after the showcase-removal/Ktor migration work: this is worse than "doesn't mention
`:shared`" — `app/src/main/java/pl/gi/codingchallenge/` now only has `ui/`, `di/`, `util/`.
`domain/` and `data/` don't exist in `:app` at all anymore; that code moved into
`shared/src/commonMain` entirely, and Retrofit itself is gone from the project (replaced by Ktor —
see `docs/kmp-knowledge/kmp-knowledge-base.md` §4). The README's tree describes a package layout
that's gone, not one that's merely incomplete.

**Why deferred:** noticed while updating README's "Known limitations" section during the
persistence (step 6) work, when the gap was smaller (just "doesn't mention `:shared`"). Fixing the
limitations note was a small, self-contained correction; rewriting the architecture section
properly was deferred to happen once the KMP work stabilized — which it now has (showcase removal
+ iOS CI both merged to `develop`).

**Fix:** an accurate module/layer diagram reflecting `:app` (`ui/`, `di/`, `util/`) + `:shared`
(`domain/`, `data/`-equivalent `remote/`/`cache/` packages), which packages live where now, and
the Hilt/Koin bridge. No longer blocked on KMP work stabilizing — it already has.

### AT-7 — `:shared` has no lint report task

| | |
|---|---|
| **Priority** | Low |
| **Component** | `:shared` — `build.gradle.kts` / Gradle plugin |

**Problem:** `com.android.kotlin.multiplatform.library` (the plugin `:shared` uses) doesn't wire
up a full `lint`/`lintDebug`-equivalent report-generating task the way `com.android.library` does
for `:app`. `:shared` only has `lintAnalyzeAndroidHostTest`, an internal analysis step with no
standalone HTML report — confirmed via `./gradlew :shared:tasks --all`.

**Why deferred:** this looks like expected behavior of the plugin (a newer, KMP-flavored Android
Gradle plugin variant with a narrower feature set), not a misconfiguration — `:shared`'s Kotlin
code still gets compiled and type-checked either way, and Detekt/ktlint-style static analysis (if
ever added) wouldn't depend on this task family. No evidence yet that the missing report is
actually costing anything.

**Fix:** the research this originally called for is now done — see
`docs/architecture/static-analysis-tools.md` for the full comparison. Short version: this specific
gap (Android Lint on `:shared`) isn't independently actionable (AGP plugin limitation, no
workaround), but ktlint + `.editorconfig` are recommended as a near-zero-cost addition regardless,
and would cover `:shared` the moment they're added since they're not Android-Lint-shaped.

### AT-9 — `docs/kmp-knowledge/kmp-knowledge-base.md` is the only file in its subdirectory

| | |
|---|---|
| **Priority** | Low |
| **Component** | Docs — `docs/kmp-knowledge/` |

**Problem:** it's the sole file under `docs/kmp-knowledge/`, unlike `docs/architecture/` which
holds multiple docs and earns its subfolder. Purely cosmetic.

**Why deferred:** noticed during a docs-accuracy review pass; not worth a churn commit on its own.

**Fix:** `git mv docs/kmp-knowledge/kmp-knowledge-base.md docs/kmp-knowledge-base.md` next time
this area is touched for another reason.

### AT-11 — `GitHubApi` never checks HTTP status; a rate-limit/error response reads as a JSON parse crash

| | |
|---|---|
| **Priority** | High |
| **Component** | `:shared` — `remote/GitHubApi.kt` |

**Problem:** Ktor 3.5.2 defaults `expectSuccess = false` and nothing in this codebase overrides it
(confirmed — no `expectSuccess`/`HttpResponseValidator` anywhere in the repo), so `.body()` runs
against every HTTP status code, not just 2xx. GitHub's real error body for a 403 rate-limit or 422
validation failure is `{"message": ..., "documentation_url": ...}`, which has none of
`GitHubSearchResponse`'s three required, no-default fields (`total_count`, `incomplete_results`,
`items`). kotlinx.serialization throws `MissingFieldException` on that, and the raw message
("Field 'total_count' is required") ends up in `SearchOutcome.Failure.message` and gets shown to
the user verbatim — instead of a clear "rate limited, try again shortly." `GitHubApiTest.kt` only
ever constructs `HttpStatusCode.OK` responses; no test builds a non-2xx one.

**Why deferred:** found during a source-code bug-hunting pass, not hit through the UI yet — needs a
real 403/422 from GitHub to surface, which is exactly the rate-limit scenario the README's "Known
limitations" already flags as a real, expected occurrence for this app.

**Fix:** add explicit status handling in `GitHubApi.kt` (either `expectSuccess = true` +
`HttpResponseValidator`, or inspect `response.status` before `.body()`), map non-2xx responses to a
clear error distinct from "the JSON didn't parse," and add a `GitHubApiTest` case constructing a
403/422 `MockEngine` response to lock the behavior in.

### AT-12 — `GitHubSearchNetworkSourceImpl` is fail-fast across two independent calls, discarding real results

| | |
|---|---|
| **Priority** | Medium |
| **Component** | `:shared` — `remote/GitHubSearchNetworkSourceImpl.kt` |

**Problem:** `search()` runs the users and repos calls as two `async {}` inside a plain
`coroutineScope` (lines 13-22), then `.await()`s both. Structured concurrency's standard fail-fast
semantics apply: if the repos call 4xx's/5xx's, `coroutineScope` cancels the users `async` too —
even if it had already completed successfully — and the whole `search()` call throws, discarding
real data the user would have benefited from. Nothing in the file explains this as a deliberate
choice, and no test exercises "one type fails, the other succeeds" at this layer —
`CachingGitHubSearchRepositoryTest` only covers total success or total failure, one layer up.

**Why deferred:** found during a source-code bug-hunting pass; low-frequency (both calls usually
succeed or both hit the same outage), but a real, silent, undocumented behavior.

**Fix:** replace `coroutineScope` with `supervisorScope` and wrap each `async` body in
`runCatching`, returning whatever partial results are available plus a signal that the search was
partial — genuinely exercises the `coroutineScope` vs. `supervisorScope` distinction, a common
coroutines interview question with a real bug in this exact file motivating it (see the matching
entry in `docs/kmp-knowledge/kmp-knowledge-base.md` §13).

### AT-13 — Whitespace-only/padded search queries aren't trimmed

| | |
|---|---|
| **Priority** | Low |
| **Component** | `:shared` — `domain/usecase/SearchAutocompleteUseCase.kt`; `:app` — `AutocompleteViewModel.kt` |

**Problem:** no `.trim()` exists anywhere in the query path (confirmed via repo-wide grep). A
3-space query (`"   "`) satisfies `query.length >= MIN_QUERY_LENGTH` (3), skips the `QueryTooShort`
gate, and gets sent to GitHub as `q="   "` — likely triggering AT-11's error-handling gap.
Separately, `"kotlin"` and `"kotlin "` are different cache keys (`CachedSearch.sq`'s primary key is
the raw query string) and trigger separate debounced network calls, silently fragmenting the cache.

**Why deferred:** found during a source-code bug-hunting pass; minor UX/cache-hygiene issue, not a
crash.

**Fix:** `.trim()` the query in `AutocompleteViewModel.onQueryChanged` (Android) and
`ContentView.scheduleSearch` (iOS) — or centrally in `SearchAutocompleteUseCase` so both platforms
get it for free — before the length check and before it reaches the cache key.

### AT-14 — "Alphabetical top-50" isn't actually guaranteed; GitHub's relevance ranking caps the candidate pool first

| | |
|---|---|
| **Priority** | Low |
| **Component** | `:shared` — `remote/GitHubApi.kt`, `domain/model/SearchLimits.kt`, `domain/ResultMerger.kt` |

**Problem:** `GitHubApi.kt` never sends a `sort` parameter (confirmed — only `q`/`per_page`), so
GitHub returns each type's results in its own best-match/relevance order, and only the first
`perPage` (50) of *that* ordering ever reaches `ResultMerger.mergeAndSort`. `SearchLimits.kt`'s own
comment frames the intent as avoiding silently dropping results below a true alphabetical top-50 —
but for a query with more than 50 matches of one type, an alphabetically-earlier match can rank
outside GitHub's relevance-based top-50 and never enter the candidate pool at all. `ResultMergerTest`
only tests the merge/sort step itself, never this upstream relevance-vs-alphabetical interaction.

**Why deferred:** found during a source-code bug-hunting pass; a real gap between the code's stated
intent and its actual guarantee, but only visible for queries with a large match count, and not a
crash.

**Fix:** either document the actual guarantee accurately (relevance-ranked top-50, then
alphabetized — not a true alphabetical top-50), or fetch more pages when `total_count` exceeds
`perPage` and merge across them before capping, if a true top-50 is actually wanted.

### AT-15 — iOS's error state has no retry; Android's does

| | |
|---|---|
| **Priority** | Medium |
| **Component** | `iosApp` — `ContentView.swift` |

**Problem:** `ContentView.swift`'s `.error(let message)` case (lines 67-69) renders red text with
no interactive affordance. Android's equivalent (`AutocompleteViewModel.retry()`) is wired to a
real button in `SuggestionPanel.kt`'s `ErrorState`. On iOS the only way to re-trigger a failed
query is to edit the text field away and back, since `scheduleSearch` only fires from
`.onChange(of: queryText)`. No iOS tests exist at all (`find iosApp -iname "*Test*"` returns
nothing), so this platform asymmetry is invisible to any test suite.

**Why deferred:** found during a source-code bug-hunting pass; a real feature-parity gap, not a
crash — the iOS app is otherwise fully functional, just missing this one affordance.

**Fix:** add a retry `Button` to the `.error` case in `ContentView.swift`, calling the same
`scheduleSearch(for: queryText)` path a text-field edit would trigger.

---

## Resolved

- **AT-5** — `docs/PROJECT_ANALYSIS.md` deleted outright (superseded by
  `docs/kmp-knowledge/kmp-knowledge-base.md` + this file, which are actively maintained).
- **AT-8** — the linked Artifact republished at the same URL as v2, redrawn to match current
  `develop` (network source/decorator/GitHub API box moved to the `:shared` swimlane, a third
  `iosApp` swimlane added for iOS's direct-call bypass of the use case). `diagramming-tools.md`'s
  own prose updated to match and to flag the re-verify-periodically lesson for next time.
- **AT-10** — `docs/architecture/github-search-caching-decision.md` now has a "Superseded,
  partially" callout at the top pointing to `docs/kmp-knowledge/kmp-knowledge-base.md` §4/§6 for
  current file locations; the decision/reasoning body left untouched as valid history.
- **AT-6** — fixed in the same pass that removed "step N" guide references from source comments
  (the guides no longer exist in the repo): the `iosArm64()`/`iosSimulatorArm64()` comment in
  `shared/build.gradle.kts` no longer claims an "empty skeleton," and correctly describes what the
  targets actually compile now.

*(move a ticket here once actually fixed, with a one-line pointer to the commit/PR that did it)*
