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

### AT-5 — `docs/PROJECT_ANALYSIS.md` is a stale pre-migration snapshot

| | |
|---|---|
| **Priority** | Medium |
| **Component** | Docs — `docs/PROJECT_ANALYSIS.md` |

**Problem:** the doc is dated 2026-09-15 and describes a version of the repo that no longer
exists: claims "no iOS target yet" (two iOS targets exist now, `iosArm64`/`iosSimulatorArm64`),
describes `UnitConverterActivity`/the `catfact` package as present (both deleted in the
showcase-removal work), and claims "two networking stacks are present by design: Retrofit+OkHttp
in `:app` ... and Ktor in `:shared`" (Retrofit is fully removed — verified via repo-wide grep,
only stray comments mention it). It also references the untracked `kmp-interview-prep/` directory,
which no longer exists (renamed and moved into `docs/kmp-knowledge/`).

**Why deferred:** the doc was a point-in-time analysis snapshot, not something with an update
mechanism — nothing currently re-generates or re-verifies it after the codebase moves on.

**Fix:** either regenerate it fresh against current `develop`, or delete it outright — this file
and `docs/kmp-knowledge/kmp-knowledge-base.md` already cover the current architecture and known
gaps more accurately and are actively maintained.

### AT-6 — `shared/build.gradle.kts`'s iOS-targets comment is stale

| | |
|---|---|
| **Priority** | Low |
| **Component** | `:shared` — `build.gradle.kts` |

**Problem:** the comment above `iosArm64()`/`iosSimulatorArm64()` still reads "Step 8: iOS
targets. Empty skeleton for now — no iosMain code yet, this pass only proves the
Gradle/Kotlin-Native toolchain resolves." `shared/src/iosMain/` now has four real files
(`Platform.ios.kt`, `IosPlatformModule.kt`, `KoinBootstrap.kt`, `KoinHelper.kt`) — it's no longer
an empty skeleton.

**Why deferred:** noticed during a docs-accuracy review pass, not the iOS work itself; the comment
was accurate when written and just wasn't revisited once later steps filled `iosMain` in.

**Fix:** a one-line comment update — the cheapest item in this file to close out.

### AT-7 — `:shared` has no lint report task

| | |
|---|---|
| **Priority** | Low |
| **Component** | `:shared` — `build.gradle.kts` / Gradle plugin |

**Problem:** `com.android.kotlin.multiplatform.library` (the plugin `:shared` uses) doesn't wire
up a full `lint`/`lintDebug`-equivalent report-generating task the way `com.android.library` does
for `:app`. `:shared` only has `lintAnalyzeAndroidHostTest`, an internal analysis step with no
standalone HTML report — confirmed via `./gradlew :shared:tasks --all` while writing the step 7 CI
guide.

**Why deferred:** this looks like expected behavior of the plugin (a newer, KMP-flavored Android
Gradle plugin variant with a narrower feature set), not a misconfiguration — `:shared`'s Kotlin
code still gets compiled and type-checked either way, and Detekt/ktlint-style static analysis (if
ever added) wouldn't depend on this task family. No evidence yet that the missing report is
actually costing anything.

**Fix:** would need research into whether the AGP KMP library plugin gains full lint support in a
future version, or whether a separate static-analysis tool should be added to `:shared` directly
instead of chasing parity with `:app`'s lint setup.

### AT-8 — `docs/architecture/diagramming-tools.md`'s worked example lives outside git

| | |
|---|---|
| **Priority** | Low |
| **Component** | Docs — `docs/architecture/diagramming-tools.md` |

**Problem:** the doc's one "worked example" diagram is a link to a published Claude Artifact, not
a file in this repo. That's fine as a reference, but it's the only doc in `docs/` with content
that can drift from the code (or disappear) with no diff to catch it — the doc says as much itself
("not version-controlled with the code... treat it as a linked reference").

**Why deferred:** noticed during a docs-accuracy review pass; low priority since the doc already
flags this limitation honestly and doesn't claim to be the source of truth.

**Fix:** nothing necessarily — worth revisiting only if that Artifact link ever goes stale or the
team wants the actual architecture diagram versioned in-repo (e.g. as a Mermaid fence, per the
doc's own recommendation for anything meant to stay maintained).

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

---

## Resolved

*(move a ticket here once actually fixed, with a one-line pointer to the commit/PR that did it)*
