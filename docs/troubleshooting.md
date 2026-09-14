# Troubleshooting / Backlog

Known gaps and rough edges noticed during work on this repo, deliberately **not** fixed at the
time they were found — either out of scope for the task in progress, or not yet judged worth the
effort. Revisit each entry and decide then whether it's worth solving; this file is a record of
"noticed and deferred," not a commitment to fix.

Each entry: what's wrong, why it wasn't fixed immediately, and what fixing it would actually
involve.

---

## Open

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
