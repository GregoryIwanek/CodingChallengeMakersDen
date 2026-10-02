# Modularization: When and How

**Status:** reference, not a decision. Guidance for when to split a Gradle project into more
modules, what it costs, and how it applies to this repo and to a future web + Android shop app.
Revisit when one of the signals below appears.

**Short answer:** split when a module boundary solves a problem you have now (slow builds, team
overlap, broken layering, code shared across apps), not by default. This repo is already split
where it matters; a shop app has real reasons to start with a few modules.

---

## When it pays off

| Signal | Why modules help |
| --- | --- |
| Incremental builds feel slow (roughly > 1 min) | Gradle rebuilds only the modules a change touches, and builds independent ones in parallel |
| Several people or teams in the same code | Boundaries cut merge conflicts and make ownership clear |
| Layering rules keep getting broken (UI calling the network layer) | A missing module dependency makes the rule a compile error instead of a review comment |
| Code shared across apps or platforms | Shared code has to be its own module anyway (why `:shared` exists here) |
| Features that ship, test or toggle independently | Each one can be tested and previewed alone |

None of these signals present → keep the module count as it is.

## What it costs

| Cost | Mitigation |
| --- | --- |
| Gradle config per module, which drifts if copied | Convention plugins in `build-logic/` from the first extra module |
| DI wiring across module boundaries | One DI module per Gradle module; in this repo that means Hilt modules plus the Koin bridge |
| Navigation between features that can't depend on each other | `:app` owns the nav graph; features expose routes or entry composables |
| More files and indirection for the same behavior | Split only along a real boundary, not per screen |
| Resources and test fixtures duplicated across modules | A `:core:testing` module once two modules need the same fixtures |

---

## This repo

| Module | Holds |
| --- | --- |
| `:app` | Thin shell: demo screen, the app's `NavHost`, Hilt app + Koin bridge |
| `:feature:autocomplete` | The search component (bar, panel, `AutocompleteViewModel`); public entry point `GitHubAutocompleteBarComponent` |
| `:feature:detail` | Full-screen repo/user detail; exposes only `navigateToDetail()` / `detailScreen()` |
| `:shared` | KMP domain + data: use case, repositories, Ktor client, SQLDelight cache |
| `build-logic/` | Convention plugins `codingchallenge.android.{library,compose,paparazzi,hilt}` (included build, not a module) |
| `iosApp` | SwiftUI app consuming `:shared` (Xcode project, not a Gradle module) |

The split that matters is platform UI vs. shared logic. The two feature modules were added as
deliberate practice, not because a signal above appeared. They follow the shop layout's rules:
each depends on `:shared`, neither depends on the other, and `:app` is the only place that
connects them (autocomplete's `onItemClick` → `navigateToDetail`).

### What the first feature module taught

- **Convention plugins pay off from module one.** `:feature:detail`'s build script is a few
  lines. SDK levels, Java target, ktlint, Compose setup and Paparazzi wiring (including the
  Gradle 9 report workaround) come from `build-logic/`. Plugin
  versions stay in the root catalog, which `build-logic/settings.gradle.kts` reuses.
- **Each module has its own `R`.** `:app`'s theme, colors and `ResourceUtil` helpers are
  invisible from a feature. Paparazzi tests there need a platform theme
  (`android:Theme.Material.Light.NoActionBar`) plus a `MaterialTheme` wrapper.
- **Navigation is the boundary.** The route class is `internal`; the module's public API is a
  `NavController` extension to go there and a `NavGraphBuilder` extension to register the screen.
  Another feature could navigate to it only through `:app`, never by importing it.
- **Each new module needs its own `.gitignore`** (`/build`). The root one only ignores the
  top-level `build/`.

### What extracting `:feature:autocomplete` taught

- **Move resources and tests with the code.** `git mv` keeps history; every `autocomplete_*`
  string, dimen and color moved too, and `:app` keeps only `demo_*` and its theme.
- **Renaming the package renames the goldens.** Paparazzi names files after package + class, so
  "screenshots don't change" has to be proven by byte-comparing old vs new PNGs, not by
  `verifyPaparazziDebug` alone, which tolerates 0.1% difference.
  That comparison also caught 3 goldens left stale by an earlier fix, which got their own commit.
- **The Paparazzi host theme matters even for fixed-color UI.** A platform theme changed real
  pixels. A debug-only copy of `:app`'s theme (`src/debug/res`, Material Components as
  `debugImplementation`) keeps them identical without shipping it.
- **Hilt works across modules without extra wiring.** `@HiltViewModel` in the feature, bindings
  in `:app`'s bridge; the `.hilt` convention plugin applies Hilt + KSP to the feature.
- **Cross-module tests need public hooks.** `AutocompleteTestTags` went from `internal` to public
  so `:app`'s end-to-end `DetailFlowTest` can still target the bar.
- **Remove shared helpers from the host first.** `DemoScreen` switched to Compose built-ins
  before the move, so `ResourceUtil` could become `internal` to the feature and every commit
  built.

### Practice exercise: extract `:feature:autocomplete` (done)

Worth doing as a learning step before the shop app. It's the same move at small scale, and it
hits every friction point (DI, resources, Paparazzi, CI). The README already says the component
would ship as its own module in a real project. With `:feature:detail` in place, it would also
prove the no-feature-to-feature rule for real: `:app` would be wiring two feature modules that
can't see each other.

1. Add `build-logic/` with an `android-library` convention plugin (+ a Compose one).
2. Create `:feature:autocomplete` and move in the composables, `AutocompleteViewModel`,
   `AutocompleteUiState`, test tags, its resources and its tests and goldens.
3. Keep `:app` as a thin shell: `MainActivity`, `DemoScreen`, Hilt setup.
4. Check that Paparazzi goldens still verify, androidTests still compile, and CI paths cover the
   new module.

Plan it with `/new-feature` (see [workflows](../ai/workflows.md#1-add-a-new-feature)): tag
`[REFACTOR]`, and screenshots should not change. Done; see the lessons above.

### Follow-up: fetch full details for the detail screen

Today the detail screen shows only what search returned, passed through the nav route as JSON.
GitHub's `GET /repos/{owner}/{repo}` and `GET /users/{login}` would add forks, language, topics,
followers, bio and so on.

- **Where it goes:** a new method on a `:shared` repository plus `GitHubApi` calls and DTOs.
  That's `[KMP]` work, runs iOS CI, and iOS can reuse it.
- **What changes in the feature:** the screen gains Loading/Error states and a `ViewModel` (so
  Hilt enters `:feature:detail`). The route can shrink to an id plus type, with the search
  result shown first and the full details filled in when the request returns.
- **Shop parallel:** a catalog list returns a product summary; the product page fetches the full
  product. It's the same two-step load.

### Follow-up: resource helpers across modules

Every module now uses Compose's built-ins (`stringResource`, `dimensionResource`,
`colorResource`); the old one-line aliases (`strRes`/`dimRes`/`colRes`) are gone. The one helper
with real logic, `spRes` (reads an sp dimen without applying font scale twice), stays `internal`
to `:feature:autocomplete`, its only user; `:feature:detail` uses `MaterialTheme.typography`.

| Option | Pros | Cons |
| --- | --- | --- |
| Compose built-ins in each module (current) | No extra module; standard API every Android dev knows | `spRes`'s font-scale fix has to be remembered wherever sp dimens are read |
| `:core:ui` module holding `ResourceUtil` | One implementation and one test; `:app` and features share it | One more module and build file; every helper becomes public API |
| Copy the helpers into each module | Same call style everywhere, no coupling | Copies drift; a fix has to be applied N times |

**The proper way:** shared UI helpers and design tokens belong in a `:core:ui` /
`:core:designsystem` module, but only once two or more modules really share them. With one
feature that reads only strings and dimens, the built-ins are enough. In the shop app,
`:core:designsystem` should exist from the start. Its contents should be theme tokens
(colors, type scale, spacing) and shared components (buttons, cards, price labels), not thin
wrappers around `stringResource`. Features then read `MaterialTheme` / custom
`CompositionLocal`s, and text sizes come from the type scale, so an sp helper like `spRes`
isn't needed.

---

## A future web + Android shop app

A shop has reasons to modularize from day one: web and Android need the same domain and data,
and its features (catalog, cart, checkout) are clearly separate.

```
build-logic/            convention plugins (android-library, kmp-library, compose)
:shared:model           Product, Cart, Order …            (KMP)
:shared:data            API client, repositories, cache    (KMP)
:core:designsystem      theme, buttons, cards              (Android; web later)
:feature:catalog        list + detail
:feature:cart
:feature:checkout
:app (Android)  /  web app
```

**Dependency rules**

| Module | May depend on | Must not depend on |
| --- | --- | --- |
| `:app` | everything | — |
| `:feature:*` | `:shared:*`, `:core:*` | other `:feature:*` |
| `:core:*` | `:shared:model` | `:feature:*`, `:app` |
| `:shared:data` | `:shared:model` | any UI module |
| `:shared:model` | nothing project-internal | everything else |

Start with `catalog` and `cart`; split further (`:core:network`, `:core:database`,
`:feature:account` …) only when a signal from the first table shows up.

### Decide first: what is the web side built with?

This changes the module graph more than anything else.

| Web stack | What can be shared | Effect on modules |
| --- | --- | --- |
| Compose Multiplatform for web (Kotlin/Wasm) | Domain, data **and UI** | `:feature:*` and `:core:designsystem` can be multiplatform |
| TypeScript framework (React, Next.js …) | Mostly the backend API; optionally a KMP→JS data module | `:feature:*` stay Android-only; the web app is a separate project or folder |

---

## Checklist before adding a module

- [ ] Which signal from [When it pays off](#when-it-pays-off) does it address?
- [ ] Does it follow the dependency rules (no feature → feature)?
- [ ] Is it built from a `build-logic/` convention plugin, not a copied build file?
- [ ] Are DI bindings, resources and tests moved along with the code?
- [ ] Do CI workflows still cover it (paths filter in `ios-ci.yml` for KMP modules)?
- [ ] Does `./gradlew check` pass with unchanged Paparazzi goldens?
