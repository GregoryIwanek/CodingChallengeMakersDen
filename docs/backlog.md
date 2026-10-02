# Awaiting Tasks

Known gaps, rough edges, and stale documentation noticed during work on this repo, deliberately
**not** fixed at the time they were found — either out of scope for the task in progress, or not
yet judged worth the effort. Revisit each ticket and decide then whether it's worth solving; this
file is a record of "noticed and deferred," not a commitment to fix.

Each ticket: priority, component, what's wrong, why it wasn't fixed immediately, and what fixing
it would actually involve.

---

## Open

### AT-16 — `initialActive = true` is overridden by the bar's first focus callback

| | |
|---|---|
| **Priority** | Medium |
| **Component** | `:app` — `ui/autocomplete/FloatingSearchBar.kt` |

**Problem:** `FloatingSearchBar` reports focus as `.onFocusChanged { onActiveChange(it.isFocused) }`
(`FloatingSearchBar.kt:127`). Compose fires `onFocusChanged` once on first composition with
`isFocused = false`, which sets `active = false` and overrides `initialActive = true`. The panel
never opens unless the field really gains focus. 13 of `GitHubAutocompleteBarTest`'s 24
instrumented tests set `initialActive = true` without typing and fail on a Pixel 8a (API 36)
emulator, e.g. `loadingState_showsLoadingIndicator`, `emptyState_showsEmptyMessage`,
`successState_*`, `errorState_*`. Tests that type into the field pass.

**Why deferred:** found while running instrumented tests for the `:feature:detail` branch, which
doesn't touch the autocomplete code. No user-facing impact: only previews and tests pass
`initialActive = true`, and Paparazzi doesn't dispatch the initial focus event, so goldens still
show the panel.

**Fix:** react only to real focus transitions, e.g. track the last focus state and call
`onActiveChange` only when it changes (skipping the initial `false`), or have the tests that
need an open panel request focus. Then re-run `:app:connectedDebugAndroidTest` and expect 27/27
(including `DetailFlowTest`).

---

## Resolved

- **AT-5** — `docs/PROJECT_ANALYSIS.md` deleted outright (superseded by
  `docs/kmp-knowledge-base.md` + this file, which are actively maintained).
- **AT-8** — the linked Artifact republished at the same URL as v2, redrawn to match current
  `develop` (network source/decorator/GitHub API box moved to the `:shared` swimlane, a third
  `iosApp` swimlane added for iOS's direct-call bypass of the use case). `diagramming-tools.md`'s
  own prose updated to match and to flag the re-verify-periodically lesson for next time.
- **AT-10** — `docs/architecture/github-search-caching-decision.md` now has a "Superseded,
  partially" callout at the top pointing to `docs/kmp-knowledge-base.md` §4/§6 for
  current file locations; the decision/reasoning body left untouched as valid history.
- **AT-6** — fixed in the same pass that removed "step N" guide references from source comments
  (the guides no longer exist in the repo): the `iosArm64()`/`iosSimulatorArm64()` comment in
  `shared/build.gradle.kts` no longer claims an "empty skeleton," and correctly describes what the
  targets actually compile now.
- **AT-11** — fixed via PR #5 (`kmp_github_api_error_handling`, merged into `develop`).
  `GitHubApi.kt` now checks HTTP status before deserializing, mapping non-2xx responses to a new
  `GitHubApiException` sealed hierarchy instead of letting them fail `GitHubSearchResponse`'s
  required fields. Full record in `docs/architecture/github-api-error-handling.md`.
- **AT-4** — fixed via PR #6 (`kmp_readme_architecture_fix`, merged into `develop`). README's
  architecture section rewritten to show the real `:app`/`:shared` split (`ui/`/`di/`/`util/` in
  `:app`; `domain/`/`remote/`/`cache/` in `:shared`), replacing the tree that described a package
  layout that no longer exists. Also added a note on `androidMain`/`iosMain` pointing to
  `docs/kmp-knowledge-base.md` §1 for the full source-set breakdown.
- **AT-1** — fixed by adding `SearchResultItem.uniqueKey` (a `"repo:$id"`/`"user:$id"`-prefixed
  key) in `:shared`'s domain model, used by both `SuggestionPanel.kt`'s `LazyColumn` (Android) and
  `ContentView.swift`'s `List` (iOS) instead of the raw, collidable `id`. Fixed once in the shared
  domain model rather than duplicating a per-platform key workaround. New test:
  `SearchResultItemTest.kt` (`:shared/commonTest`) proves a repo and a user sharing the same `id`
  still get distinct `uniqueKey`s.
- **AT-2** — `CachingGitHubSearchRepository.search()` now catches `CancellationException` before
  the generic `catch (e: Exception)` and rethrows it. New test cancels a real in-flight coroutine
  mid-search (the fake network source now genuinely suspends via `delay`, not a synchronous
  throw) and asserts `CancellationException` propagates and `cache.get()` was never called —
  verified as a real regression test by confirming it fails without the fix, not just that it
  passes with it.
- **AT-12** — `GitHubSearchNetworkSourceImpl.search()` now uses `supervisorScope` +
  a per-branch `catchingCancellationAware` (a `runCatching` that doesn't swallow
  `CancellationException`) instead of a plain `coroutineScope`, so one branch failing no longer
  discards the other's real, already-fetched results. New `GitHubSearchNetworkSourceImplTest.kt`
  (didn't exist before) covers users-fail/repos-succeed, repos-fail/users-succeed, both-fail
  (throws), and cancellation. **Scoped down from the ticket's original fix text:** implemented
  "return partial results instead of discarding them" but not "plus a signal that the search was
  partial" — that would mean threading a new partial/degraded state through
  `GitHubSearchRepository`, `SearchOutcome`, and both platforms' UI, which is a real design
  decision (worth its own ADR, like `docs/architecture/github-search-caching-decision.md`) rather
  than folding into this fix. Also verified empirically, not just by rule: for this exact
  await-both shape, a plain `runCatching` turns out to be behaviorally identical to the
  cancellation-aware version, because a cancelled `Deferred`'s `.await()` throws regardless of
  what its own coroutine body caught and returned — see `catchingCancellationAware`'s comment for
  why it's kept anyway.
- **AT-9** — fixed via PR #10 (`kmp_bundle_at13_at15_at9`, merged into `develop`).
  `docs/kmp-knowledge-base.md` moved up from `docs/kmp-knowledge/` (the now-empty
  subfolder removed); all cross-references in `README.md`, this file, and the three
  `docs/architecture/*.md` docs updated to the new path.
- **AT-13** — fixed via PR #10 (`kmp_bundle_at13_at15_at9`, merged into `develop`). Trimmed in
  both real entry points, not just one: `SearchAutocompleteUseCase.kt` (Android's path) and
  `ContentView.swift`'s `scheduleSearch` (iOS's separate path, since iOS bypasses the use case
  entirely — the ticket's own "or centrally in `SearchAutocompleteUseCase` so both platforms get
  it for free" suggestion turned out to be outdated advice from before that bypass was
  discovered; fixing only the use case would have silently missed iOS). New tests:
  `whitespaceOnlyQueryStaysQueryTooShortAndNeverCallsRepository` and
  `paddedQueryIsTrimmedBeforeReachingTheRepository` in `SearchAutocompleteUseCaseTest.kt`.
- **AT-15** — fixed via PR #10 (`kmp_bundle_at13_at15_at9`, merged into `develop`). Added a retry
  `Button` to `ContentView.swift`'s `.error` case, calling `scheduleSearch(for: queryText)` — the
  same path a text-field edit would trigger. Verified by actually building the iOS app
  (`xcodebuild ... -destination 'platform=iOS Simulator,name=iPhone 17'`, matching this Mac's
  arm64 `Shared.framework` build) since no CI or test suite exercises `ContentView.swift` at all.
- **AT-7** — fixed via PR #11 (`kmp_ktlint_editorconfig`, merged into `develop`). The literal ask
  (an Android Lint report task on `:shared`) stays blocked, same as
  before: `com.android.kotlin.multiplatform.library` still doesn't expose the full `lint`/
  `lintDebug` task family, and there's no config workaround (AGP plugin limitation). Resolved via
  the practical mitigation `docs/architecture/static-analysis-tools.md` already recommended:
  ktlint (`org.jlleitschuh.gradle.ktlint`, `android_studio` code style) + `.editorconfig` added to
  both `:app` and `:shared`, wired into `check` and `android-ci.yml` as its own fast-fail step —
  this covers `:shared` (and `iosMain`/`commonMain`/`commonTest`) since it's plain Kotlin lint, not
  Android-Lint-shaped. detekt/SwiftLint/Konsist remain deliberately deferred (see that doc).
- **AT-3** — fixed via PR #12 (`kmp_at3_cache_eviction`, merged into `develop`). Row cap with LRU
  eviction, the first of the ticket's own suggested fixes. No new
  timestamp column: `CachedSearch.sq`'s `evictOldestBeyondCap` deletes rows outside the top
  `MAX_CACHED_QUERIES` (50) by `rowid`, relying on documented SQLite behavior that `INSERT OR
  REPLACE` deletes+reinserts on a conflicting key — so a fresh distinct query gets the next rowid,
  and re-upserting an existing query refreshes it to the newest rowid too, giving write-recency
  ordering for free. Called from `SearchResultCache.put()` after every `upsert`. New tests:
  `put_beyondCap_evictsOldestQueriesFirst` (confirmed to fail without the fix, not just pass with
  it) and `put_reUpsertingAnExistingQuery_refreshesItsRecency`.
- **AT-14** — fixed via PR #13 (`kmp_at14_fix_comment`, merged into `develop`). Closed as won't-fix
  on the real fix (fetching multiple pages to guarantee a true alphabetical top-50 adds complexity
  and rate-limit exposure not worth it for a low-priority, large-match-count-only edge case), but
  the misleading claim it found was real, so that part is fixed: `SearchLimits.kt`'s doc comment
  now states the actual guarantee (a true alphabetical top-50 *within GitHub's own relevance-ranked
  candidate pool*, not a true alphabetical top-50 overall) instead of overstating it. No behavior
  change.

*(move a ticket here once actually fixed, with a one-line pointer to the commit/PR that did it)*
