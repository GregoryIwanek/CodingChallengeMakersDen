# Troubleshooting / Backlog

Known gaps and rough edges noticed during work on this repo, deliberately **not** fixed at the
time they were found — either out of scope for the task in progress, or not yet judged worth the
effort. Revisit each entry and decide then whether it's worth solving; this file is a record of
"noticed and deferred," not a commitment to fix.

Each entry: what's wrong, why it wasn't fixed immediately, and what fixing it would actually
involve.

---

## Open

### `SuggestionPanel`'s LazyColumn key can collide across result types (crash)

**What's wrong:** `SuggestionPanel` merges users and repos into one list and keys each
`itemsIndexed` row on `item.id` alone (`key = { _, item -> item.id }`). `RepositoryDto.id` and
`UserDto.id` are both plain `Long`s from GitHub's REST API, but users and repos are independent
entity types with their own ID sequences — a user and a repository can legitimately share the same
numeric ID. Any search whose merged, top-50 result set contains such a pair throws
`IllegalArgumentException: Key ... was already used` from Compose's `LazyColumn`, crashing the
screen. Nothing in the current fixtures/tests uses colliding IDs across the two types, so this
hasn't surfaced in `GitHubAutocompleteBarTest` or the Paparazzi goldens.

**Why deferred:** found during a codebase-wide bug sweep, not hit organically yet — needs a live
query that happens to return a colliding pair to reproduce, which is a matter of when, not if,
against real GitHub data.

**What fixing it would involve:** disambiguate the key by result type, e.g.
`key = { _, item -> "${item::class.simpleName}:${item.id}" }`, or add a type discriminator to
`SearchResultItem.id` itself so downstream consumers don't have to remember the collision risk.

### `CachingGitHubSearchRepository` can swallow `CancellationException`

**What's wrong:** `CachingGitHubSearchRepository.search()` wraps the network call in
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

**What fixing it would involve:** re-throw cancellation before the generic catch, e.g.
`catch (e: CancellationException) { throw e } catch (e: Exception) { cache.get(query) ?: throw e }`,
plus a test asserting a cancelled `search()` propagates cancellation instead of returning a cached
value.

### SQLDelight search cache has no eviction or size cap

**What's wrong:** `cachedSearch` (`CachedSearch.sq`) upserts one row per distinct query string with
no TTL, max-row-count, or LRU eviction. Since `SearchAutocompleteUseCase` debounces per keystroke
past 3 characters, typing one word can write several rows (`"kot"`, `"kotl"`, `"kotli"`, ...), all
kept forever. Over normal usage the table grows unbounded on-device storage with no cleanup path.

**Why deferred:** found during a codebase-wide bug sweep; not a crash or correctness bug, and the
README's "Known limitations" already flags the cache as non-general-purpose — this is a related but
distinct gap (unbounded growth, not staleness).

**What fixing it would involve:** either a row cap with LRU eviction (e.g. delete oldest beyond N
rows on `upsert`), a TTL column checked on read, or deliberately deciding unbounded growth is
acceptable for this cache's realistic lifetime and documenting that choice.

### README architecture/module-tree section is stale

**What's wrong:** the README's architecture/module-tree section still describes `domain/` and
`data/` as living entirely under `pl.gi.codingchallenge`. It doesn't mention `:shared` at all,
even though the KMP work has since moved real domain code (`GitHubSearchRepository`,
`SearchResultItem`, `ResultMerger`, the caching layer) into `shared/src/commonMain`.

**Why deferred:** noticed while updating README's "Known limitations" section during the
persistence (step 6) work. Fixing the limitations note was a small, self-contained correction;
rewriting the architecture section properly is a bigger documentation pass, and the plan is to do
that later as an AI-assisted documentation exercise rather than a quick patch now.

**What fixing it would involve:** an accurate module/layer diagram reflecting `:app` + `:shared`,
which packages now live where, and the Hilt/Koin bridge — likely worth doing once the KMP work
(through at least step 7, possibly step 8's iOS module) has stabilized, so the diagram doesn't need
another rewrite immediately after.

### `:shared` has no lint report task

**What's wrong:** `com.android.kotlin.multiplatform.library` (the plugin `:shared` uses) doesn't
wire up a full `lint`/`lintDebug`-equivalent report-generating task the way `com.android.library`
does for `:app`. `:shared` only has `lintAnalyzeAndroidHostTest`, an internal analysis step with no
standalone HTML report — confirmed via `./gradlew :shared:tasks --all` while writing the step 7 CI
guide.

**Why deferred:** this looks like expected behavior of the plugin (a newer, KMP-flavored Android
Gradle plugin variant with a narrower feature set), not a misconfiguration — `:shared`'s Kotlin
code still gets compiled and type-checked either way, and Detekt/ktlint-style static analysis (if
ever added) wouldn't depend on this task family. No evidence yet that the missing report is
actually costing anything.

**What fixing it would involve:** would need research into whether the AGP KMP library plugin
gains full lint support in a future version, or whether a separate static-analysis tool should be
added to `:shared` directly instead of chasing parity with `:app`'s lint setup.

---

## Resolved

*(move an entry here once actually fixed, with a one-line pointer to the commit/PR that did it)*
