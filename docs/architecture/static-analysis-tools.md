# Static Analysis & Style Tools

**Status:** reference, not a decision — none of these are installed today (confirmed via a
repo-wide search: no `ktlint`/`detekt`/`SwiftLint`/`Konsist` config anywhere, no `.editorconfig`).
Revisit if the codebase grows enough that manual review alone stops being enough. Related:
`docs/backlog.md` AT-7 (`:shared` has no Android Lint report task — a narrower, already-open gap
this doc's Android Lint row also covers).

**Current baseline, so the table below has something real to compare against:** Android Lint is
fully wired for `:app` (`lintDebug`, `lintRelease`, `lintFix`, ... — 20+ generated tasks, confirmed
via `./gradlew :app:tasks --all`) and runs as part of `check`, so `:app`'s Compose/Android code
already gets real static analysis. `:shared` gets none (AT-7). `iosApp`'s two Swift files get none.
Codebase size as of this doc: 49 Kotlin files / ~3,100 lines (`:app` + `:shared`), 2 Swift files /
144 lines (`iosApp`) — small enough that "is this worth the setup cost *yet*" is a real question,
not a formality.

---

## The options

| Tool | Checks | Scope here | KMP-aware? | Setup cost |
|---|---|---|---|---|
| **ktlint** | Kotlin formatting — enforces the official Kotlin style (imports, indentation, trailing commas, spacing) | `:app` + `:shared`, any Kotlin source regardless of target | Yes — operates on `.kt` files, no per-target awareness needed | Low — one Gradle plugin, sane defaults out of the box, `ktlintFormat` auto-fixes most violations |
| **detekt** | Kotlin static analysis — complexity (long methods, deep nesting), code smells, potential bugs, style | `:app` + `:shared` | Yes — same reason as ktlint | Medium — plugin + a `detekt.yml` worth actually tuning (the full default ruleset is noisy on a small, deliberately-simple codebase) |
| **Android Lint on `:shared`** | API-level usage, resource/manifest correctness, Android-specific correctness rules | `:shared`'s `androidMain` only (nothing for `iosMain`/`commonMain` — Lint is Android-specific, not a general Kotlin linter) | No — Android-only, unrelated to the iOS side | Blocked, not "low/medium" — `com.android.kotlin.multiplatform.library` doesn't expose the full lint task family AGP's `com.android.library` does (this is AT-7, not new information) |
| **SwiftLint** | Swift style/convention checks | `iosApp` (2 files, 144 lines today) | N/A — Swift-only, not a KMP tool | Low, but the CI question is the interesting part: SwiftLint doesn't need Xcode — it can run on `ubuntu-latest` via the Swift toolchain, avoiding the 10× macOS multiplier `docs/backlog.md`'s CI-cost tickets already flagged for `ios-ci.yml` |
| **Konsist** | Architecture-rule assertions written as ordinary Kotlin tests (e.g. "no class in `domain/` imports from `data/`") | Cross-module — the one tool here that can actually test the layering rules this repo already documents in prose | Yes — pure Kotlin/JVM test library, runs in `commonTest`-adjacent JVM test source sets | Medium — no config file, but each rule is code you write and must keep correct |
| **`.editorconfig`** | Baseline formatting (indent style/size, line endings, trailing whitespace, final newline) | All files, all languages including `.swift`/`.md`/`.yml` | N/A — universal, IDE-level | Trivial — one file, IDE-enforced, zero CI wiring needed unless paired with a linter that reads it |

## Is it worth it, for this repo specifically

- **ktlint — yes, add it.** Near-zero setup cost, auto-fixable, and catches real drift (import
  order, spacing) that's currently caught by nothing but manual review. Worth adding regardless of
  codebase size, precisely *because* it's cheap.
- **`.editorconfig` — yes, add it alongside ktlint.** Costs one file, no reason not to.
- **detekt — not yet, revisit later.** At ~3,100 lines with one active contributor, the default
  ruleset's complexity/smell warnings are more likely to be noise than signal — this codebase has
  been kept deliberately simple on purpose (see `docs/kmp-knowledge/kmp-knowledge-base.md`'s
  "no premature abstraction" thread throughout). Worth reconsidering once the codebase is large
  enough, or has enough contributors, that a style/complexity drift could plausibly go unnoticed in
  review. A cheap trial run: enable it with just the `style` rule set, skip `complexity`/`coroutines`
  rule sets until they'd actually catch something real here.
- **Android Lint on `:shared` — not actionable right now.** This is AT-7, not a new decision: the
  AGP plugin `:shared` uses simply doesn't expose the task, and there's no config workaround.
  Revisit only if a future AGP version changes this.
- **SwiftLint — low priority given current size (144 lines), but a good learning exercise.** The
  interesting part isn't the linter itself, it's proving SwiftLint can run in CI on `ubuntu-latest`
  without ever touching a macOS runner — a concrete, cheap way to extend the "verify, don't guess"
  discipline this project already applies to CI cost (see `docs/backlog.md`'s iOS CI cost tickets)
  to a second tool.
- **Konsist — the most interesting one for this project specifically, not the most urgent.** This
  repo already has real, documented architecture rules that are currently enforced by nothing but
  developer discipline and code review: "`ui`/`data` → `domain`, never reverse" (README), the
  `:app`/`:shared` module boundary itself, "no concrete classes across the network/cache split"
  (`docs/architecture/github-search-caching-decision.md`). Konsist could turn 2-3 of those into
  actual failing tests instead of prose. Not urgent because nothing has violated these rules yet —
  but "encode an architecture rule as a test, not a comment" is a genuinely useful KMP-adjacent
  skill to have hands-on experience with, independent of whether this specific repo strictly needs
  it today.

## Recommendation

If picking one thing to actually add right now: **ktlint + `.editorconfig`** — both are effectively
free, and there's no version of "codebase too small for this" that applies to a formatter.
Everything else on this list is a **"revisit when"**, not a **"do now"**: detekt when the codebase
or contributor count grows, Konsist when there's a specific layering rule worth locking in as a
test (or just as a standalone learning exercise), SwiftLint when `iosApp` grows past a couple of
files, and Android Lint on `:shared` only if AGP itself changes.
