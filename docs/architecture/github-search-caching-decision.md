# Design Discussion: Caching Architecture for `GitHubSearchRepository`

> **Superseded, partially:** the *decision* below (segregated interfaces, decorator pattern, why
> Option 2 over 1/3) is still accurate and still the reasoning behind the current design. But the
> **"What actually changed"/"Verification" sections describe a Hilt-based end state that no longer
> exists** — a later Ktor migration moved `GitHubSearchNetworkSourceImpl` and
> `CachingGitHubSearchRepository` from `:app`/Hilt into `:shared`/Koin (Retrofit is gone
> entirely; `RepositoryModule.kt`/`NetworkModule.kt` no longer exist). For current file locations
> and wiring, see `docs/kmp-knowledge/kmp-knowledge-base.md` §4 and §6.

**Status:** decided and implemented. **Context:** after adding a SQLDelight-backed cache in
front of `GitHubSearchRepository` (to soften GitHub's documented 10 req/min rate limit — see the
README's "Known limitations"), the caching logic was implemented as a `CachingGitHubSearchRepository`
that decorates the existing Retrofit-backed repository, both sharing one `GitHubSearchRepository`
interface. This doc records the design discussion that followed, the alternatives considered, and
why the final shape looks the way it does.

---

## The question that started this

> Is it OK to have two repositories implementing the same interface, one taking the other as a
> constructor parameter? Why not two separate interfaces, one per repository?

Then, once the "decorator" answer was accepted:

> What if the ViewModel didn't know about this abstraction at all, and we moved it to a use case
> instead?

And finally, after being shown that the ViewModel (and the use case) already don't know this
exists:

> I still believe we need to discuss the option of a network repository and a cache repository —
> perhaps I don't fully understand the advantages of the current approach.

That last question is the right one, and it's what this doc actually answers.

---

## Clarifying the question: two independent decisions, not one

"Two repositories instead of one" bundles two separate design axes that are worth pulling apart:

1. **How many *interfaces* represent the data sources?** — one unified `GitHubSearchRepository`,
   or segregated ones (a narrow "raw network access" contract + a narrow "cache" contract)?
2. **Where does the fallback *policy* live** — try network, fall back to cache on failure? Inside
   a repository-layer decorator, or inside the use case?

These are independent. You can split the interfaces *and* still hide the orchestration behind one
repository-facing contract. Three real shapes follow from this.

### Option 1 — one interface, decorator holds the concrete network class

```kotlin
interface GitHubSearchRepository {
    suspend fun search(query: String, perTypeLimit: Int = MAX_RESULTS): List<SearchResultItem>
}
class GitHubSearchRepositoryImpl(private val api: GitHubApi) : GitHubSearchRepository { ... }
class CachingGitHubSearchRepository(
    private val network: GitHubSearchRepositoryImpl, // concrete class - the actual flaw here
    private val cache: SearchResultCache,             // concrete class too
) : GitHubSearchRepository { /* try/catch fallback */ }
```
This was the as-built state right after adding the cache. The repository/decorator split was
already correct; what was actually wrong was that `CachingGitHubSearchRepository` depended on two
*concrete classes* instead of abstractions — a Dependency Inversion violation, not an Interface
Segregation one.

### Option 2 — segregated interfaces, orchestration *still* in a repository decorator (chosen)

```kotlin
interface GitHubSearchRepository {           // the one thing the domain layer knows about
    suspend fun search(query: String, perTypeLimit: Int = MAX_RESULTS): List<SearchResultItem>
}
interface GitHubSearchNetworkSource {        // narrower - "raw network access," nothing else
    suspend fun search(query: String, perTypeLimit: Int): List<SearchResultItem>
}
interface GitHubSearchCache {
    suspend fun get(query: String): List<SearchResultItem>?
    suspend fun put(query: String, results: List<SearchResultItem>)
}

class GitHubSearchNetworkSourceImpl(private val api: GitHubApi) : GitHubSearchNetworkSource { ... }
class SearchResultCache(driver: SqlDriver) : GitHubSearchCache { ... }
class CachingGitHubSearchRepository(
    private val network: GitHubSearchNetworkSource,
    private val cache: GitHubSearchCache,
) : GitHubSearchRepository { /* same try/catch fallback */ }
```
`GitHubSearchRepositoryImpl` no longer claims to fulfill the domain-facing contract at all — it's
renamed to `GitHubSearchNetworkSourceImpl` and only claims "I can do raw network search." Only
`CachingGitHubSearchRepository` implements `GitHubSearchRepository`. `SearchAutocompleteUseCase`
is untouched either way — it only ever depended on `GitHubSearchRepository`.

### Option 3 — segregated interfaces, orchestration moves to the use case

```kotlin
class SearchAutocompleteUseCase(
    private val network: GitHubSearchNetworkSource,
    private val cache: GitHubSearchCache,
) {
    private suspend fun search(query: String): List<SearchResultItem> = try {
        val fresh = network.search(query, MAX_RESULTS)
        cache.put(query, fresh); fresh
    } catch (e: Exception) {
        cache.get(query) ?: throw e
    }
    // invoke() calls this instead of repository.search(query)
}
```
`GitHubSearchRepository` disappears entirely — nothing needs it. The use case now explicitly
knows a network and a cache exist, and owns the policy for reconciling them.

---

## Comparison

| | **Option 1** (as-built) | **Option 2** (chosen) | **Option 3** |
|---|---|---|---|
| Interfaces | 1 | 3 | 2 |
| Where the fallback policy lives | Data layer (decorator) | Data layer (decorator) | Domain layer (use case) |
| Does `SearchAutocompleteUseCase` know caching exists? | No | No | **Yes** |
| Does `AutocompleteViewModel` know? | No | No | No |
| SRP: fetching vs. fallback policy | Mixed until `network`/`cache` are typed as interfaces | Cleanly separated | Cleanly separated, but split across the use case's two dependencies instead of one dedicated class |
| Can `CachingGitHubSearchRepository`'s fallback logic be unit-tested without Retrofit/SQLite? | No — needs the concrete classes | **Yes** — fakes of two narrow interfaces | N/A - logic lives in the use case instead |
| Consistent with this repo's documented layering (`ui`/`data` → `domain`, never reverse) | Yes | Yes, and more explicit about it | **No** — domain layer now depends on infrastructure details |
| Can the UI ever show "these results are cached/stale"? | Not without changing `SearchOutcome` | Same | The only option where this is a natural, small extension |
| File/interface count for a stretch-goal feature | Fewest | Most (3 interfaces, 1 rename) | Middle (2 interfaces, but `GitHubSearchRepository` itself is deleted) |

## The deciding question

Whether this belongs in the repository or the use case comes down to one thing: **is
"fall back to cache on failure" infrastructure, or a product decision?**

`SearchOutcome` has exactly one `Success` case, with no "was this cached" flag anywhere in it —
the UI is architecturally blind to the distinction today. That's the concrete signal that this is
infrastructure, not a business rule the domain layer needs to reason about. If a future
requirement ever wants to surface "showing cached results, retrying…" in the UI, *that's* the
trigger to revisit Option 3 — the policy would have become a real product decision at that point,
not before.

## Decision

**Option 2.** Segregating the interfaces gets the concrete, provable benefit that motivated this
whole discussion — `CachingGitHubSearchRepository`'s fallback policy is now unit-tested with zero
Retrofit/SQLite involved (`CachingGitHubSearchRepositoryTest`, ~1.5s, pure MockK fakes of
`GitHubSearchNetworkSource`/`GitHubSearchCache`) — without moving an infrastructure concern into
the domain layer, which would have broken this repo's own documented dependency-direction rule.

---

## What actually changed

- **New:** `GitHubSearchNetworkSource`, `GitHubSearchCache` (both `:shared/domain/repository/`).
- **Renamed** (history preserved via `git mv`): `GitHubSearchRepositoryImpl` →
  `GitHubSearchNetworkSourceImpl`, now implementing `GitHubSearchNetworkSource` instead of
  `GitHubSearchRepository`; its test class renamed to match.
- **`SearchResultCache`** now implements `GitHubSearchCache`; registered in Koin under that
  interface type (`single<GitHubSearchCache> { SearchResultCache(...) }`), not the concrete class
  — Koin looks definitions up by the exact registered type, so this had to be explicit.
- **`CachingGitHubSearchRepository`** now depends on the two interfaces, not
  `GitHubSearchNetworkSourceImpl`/`SearchResultCache` directly.
- **`RepositoryModule`** gained a second `@Binds` (`GitHubSearchNetworkSourceImpl` →
  `GitHubSearchNetworkSource`); the existing one now reads more clearly since
  `CachingGitHubSearchRepository` is unambiguously the only thing the domain layer ever sees as
  `GitHubSearchRepository`.
- **`SharedKoinBridgeModule`** now bridges `GitHubSearchCache` (interface) instead of
  `SearchResultCache` (concrete class).
- **New test:** `CachingGitHubSearchRepositoryTest` — 3 cases (success caches the result, failure
  with a cache hit falls back, failure with a cache miss rethrows), proving live-on-device
  behavior already manually verified in step 6 is now also covered by fast JVM unit tests.

## Verification

- `./gradlew check` green.
- All pre-existing tests behaviorally unchanged; `GitHubSearchNetworkSourceImplTest` needed one
  real fix — `GitHubSearchNetworkSource.search()` has no default `perTypeLimit` (unlike the
  domain-facing `GitHubSearchRepository`, which does), so two call sites relying on the old
  default needed an explicit value.
- Runtime-verified on-device: a real search still resolves through the full chain (Hilt →
  `GitHubSearchNetworkSourceImpl` → Retrofit; Hilt/Koin bridge → `GitHubSearchCache` →
  `SearchResultCache`; both combined by `CachingGitHubSearchRepository`), no crash, real results —
  confirming the DI rewiring actually works, not just compiles.
