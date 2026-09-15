# Project Analysis

High-level snapshot of `CodingChallenge` as of 2026-09-15. Scope: `:app`, `:shared`,
build/CI, and dependency setup. The untracked `kmp-interview-prep/` directory is out
of scope by request.

## Overview

A Kotlin Multiplatform Android app (Gradle modules `:app` + `:shared`, no iOS target
yet) built around one polished, reusable Compose component — a GitHub users/repos
autocomplete search bar — plus two smaller KMP spike features (a unit converter and
a cat-facts screen) that exercise the `:shared` module's networking/DB/DI stack.
~3,570 lines of Kotlin across 46 files, 98 commits.

## Architecture

- **Layering:** Clean Architecture + MVVM, `ui`/`data` → `domain`, enforced by
  dependency direction rather than tooling. `:app` holds Android/Compose-only code
  (UI, Hilt DI, Retrofit); `:shared` holds multiplatform-safe domain and data code
  (Koin DI, Ktor, SQLDelight), bridged into Hilt via `SharedKoinBridgeModule`.
- **Caching:** `CachingGitHubSearchRepository` decorates a `GitHubSearchNetworkSource`
  with a `GitHubSearchCache` (SQLDelight-backed), network-first with cache-as-fallback.
  This shape is the result of a deliberate refactor away from an earlier version that
  depended on concrete classes instead of interfaces — see
  `docs/architecture/github-search-caching-decision.md`, a good example of the kind
  of design-decision record worth keeping as the codebase grows.
- **Two DI containers coexist:** Hilt (`:app`) and Koin (`:shared`), intentionally
  bridged rather than unified — reasonable for a KMP module that needs to stay
  Android-DI-framework-agnostic, but it's a second DI mental model a new contributor
  has to learn.
- **Undocumented surface area:** `UnitConverterActivity`/`UnitConverterScreen` and
  the `catfact` package in `:shared` aren't mentioned in the README at all — they
  read as KMP-stack exercises (Ktor client, SQLDelight, Koin wiring) rather than
  product features, but nothing in the repo currently marks them as such.

## Code quality / tech debt

- **Known, tracked debt** (see `docs/troubleshooting.md`): the README's architecture
  section is stale — it still shows `domain/` and `data/` living entirely under
  `:app`, but confirmed against the actual file tree, `SearchResultItem`,
  `ResultMerger`, `GitHubSearchRepository`, and the cache layer now live in
  `shared/src/commonMain`; `:app` only retains `domain/usecase/`. This is a real,
  currently-misleading discrepancy for anyone onboarding from the README.
  `:shared` also has no wired-up lint report task (a plugin limitation, not a
  misconfiguration per that doc).
- **No static analysis tooling:** no detekt/ktlint config found. Style consistency
  currently relies on manual review and Android Lint (`:app` only).
  Given the KMP direction, this becomes more valuable as `:shared` grows.
  Speculative — flagging as an option, not a gap.
- **No `TODO`/`FIXME` markers in source** — deferred work is tracked in
  `docs/troubleshooting.md` instead, which is a healthier pattern than scattering
  TODOs (they don't rot silently in code no one greps).
- **Test coverage looks strong and intentional:** debounce/cancellation logic tested
  with virtual time, real JSON payloads exercised through the actual serialization
  pipeline, Paparazzi golden-image tests wired into `check` so visual regressions
  fail CI, and the cache decorator specifically tested against fakes of narrow
  interfaces rather than concrete classes.

## Build & CI

- Single GitHub Actions workflow (`.github/workflows/ci.yml`): runs `./gradlew check`
  (unit tests + lint + Paparazzi) on push/PR to `main`/`develop`, JDK 21 (required by
  Paparazzi), uploads test/lint/Paparazzi reports on failure. No instrumented-test
  job — `connectedDebugAndroidTest` requires a device/emulator and is intentionally
  left to local runs.
- Recent commit history (`c6796d8`, `4b9490f`) shows the CI setup itself was recently
  verified end-to-end by deliberately breaking then reverting a `:shared` test —
  a reasonable way to confirm the report-upload path actually fires on failure.
- Root `build.gradle.kts` cleanly centralizes plugin declarations with `apply false`;
  per-module build files are otherwise idiomatic Gradle Kotlin DSL.

## Dependencies & versions

- Fully version-catalog-driven (`gradle/libs.versions.toml`), current as of the
  README: Kotlin 2.3.21, AGP 9.3.2, Compose BOM 2026.08.00, Hilt 2.60.1, Retrofit
  3.0.0, Coroutines 1.11.0 — all recent, no obviously stale majors.
  minSdk 26 / target & compile SDK 37.
  Not independently re-verified against latest upstream releases in this pass.
- **Ktor and Koin deliberately have no BOM**, each artifact pinned via its own
  `version.ref` — documented inline as a workaround for a hard-deprecated
  (error-level) `platform()` API inside a KMP `commonMain.dependencies {}` block
  (KT-58759). This is exactly the kind of non-obvious constraint worth having in a
  comment, and it already is.
- SQLDelight pinned to 2.3.2 with an inline note flagging a 2.4.0-rc1 pre-release —
  worth a follow-up once that stabilizes.
- Two networking stacks are present by design: Retrofit+OkHttp in `:app` (existing
  GitHub search) and Ktor in `:shared` (multiplatform-safe for the KMP spikes) —
  not redundant, but worth knowing before assuming there's "the" networking layer.

## Notable strengths

- Design decisions are written down where they happened (caching decision doc,
  troubleshooting/backlog doc) rather than living only in commit messages or heads —
  unusually good practice for a take-home-sized project.
- Test suite mixes unit, instrumented, and screenshot testing with a clear rationale
  for each layer, and CI enforces the JVM-runnable subset on every push.

## Suggested next steps (not actioned)

1. Refresh the README architecture section to reflect the actual `:app`/`:shared`
   split (already tracked in `docs/troubleshooting.md`).
2. Decide whether `UnitConverterActivity`/`catfact` should be documented as
   deliberate KMP-stack exercises or removed if no longer needed.
3. Revisit SQLDelight version once 2.4.0 leaves RC.
