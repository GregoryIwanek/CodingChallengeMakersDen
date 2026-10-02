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
| `:app` | Android UI: the autocomplete component, demo screen, Hilt DI |
| `:shared` | KMP domain + data: use case, repositories, Ktor client, SQLDelight cache |
| `iosApp` | SwiftUI app consuming `:shared` (Xcode project, not a Gradle module) |

This is already the split that matters: platform UI vs. shared logic. With one feature and one
developer, more modules would solve no current problem.

### Practice exercise: extract `:feature:autocomplete`

Worth doing as a learning step before the shop app. It's the same move at small scale, and it
hits every friction point (DI, resources, Paparazzi, CI). The README already says the component
would ship as its own module in a real project.

1. Add `build-logic/` with an `android-library` convention plugin (+ a Compose one).
2. Create `:feature:autocomplete` and move in the composables, `AutocompleteViewModel`,
   `AutocompleteUiState`, test tags, its resources and its tests and goldens.
3. Keep `:app` as a thin shell: `MainActivity`, `DemoScreen`, Hilt setup.
4. Check that Paparazzi goldens still verify, androidTests still compile, and CI paths cover the
   new module.

Plan it with `/new-feature` (see [workflows](../ai/workflows.md#1-add-a-new-feature)): tag
`[REFACTOR]`, and screenshots should not change.

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
