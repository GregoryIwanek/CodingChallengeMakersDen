# Plan: Extract `:feature:autocomplete` (modularization step 2)

## Context

`:feature:detail` proved the module setup (convention plugins, nav extensions wired by `:app`).
The next step from `docs/architecture/modularization.md` ("Practice exercise") is to move the
existing autocomplete out of `:app`. `:app` then wires two feature modules that can't see each
other, which proves the no-feature-to-feature rule. It's a pure `[REFACTOR]`: no behavior or pixel
change. AT-16 is fixed, so all 27 instrumented tests pass and the move can be verified.

## Decisions (Q&A)

| Question | Answer | Source |
|---|---|---|
| Module | `:feature:autocomplete`, Android library, depends on `:shared` | doc / user |
| Package | Rename `pl.gi.codingchallenge.ui.autocomplete` → `pl.gi.codingchallenge.feature.autocomplete` (`testing` subpackage kept) | user |
| Goldens | 7 PNGs get new names; re-record, then **byte-compare** each with its old file to prove zero pixel change | user |
| `ResourceUtil` | Moves into the feature as `internal` (+ `ResourceUtilTest`); `DemoScreen` switches to `stringResource`/`dimensionResource` | user |
| Paparazzi theme | Try `android:Theme.Material.Light.NoActionBar`. If any golden differs, add a minimal theme copy to the feature instead | user |
| Hilt | New `codingchallenge.android.hilt` convention plugin (hilt + ksp plugins, `hilt-android` + `hilt-compiler`) | user |
| Test tags | `AutocompleteTestTags` becomes public so `:app`'s `DetailFlowTest` can use it | user |
| Public API | Hilt-backed `GitHubAutocompleteBarComponent`, `AutocompleteViewModel` (`DemoScreen` passes keyed `hiltViewModel`s), `AutocompleteUiState`. Everything else stays internal | code |
| Resources | All `autocomplete_*` strings/plurals/dimens/colors move. `demo_*`, theme, launcher and `purple/teal/black/white` stay in `:app` | code |
| DI | `SharedKoinBridgeModule` stays in `:app` and provides the use case. Hilt aggregates across modules. `FakeSearchModule` still replaces it in tests | code |
| Branch / commits | `refactor_feature_autocomplete` off `develop`; plan commit first; small commits, each asked first; no push until told | user |
| Extras | Docs update; `.claude/settings.json` allow-rules for `:feature:*` Paparazzi tasks; user re-runs instrumented tests at the end | user |
| Out of scope | `:core:ui` / `:core:designsystem`, migrating `:app` to convention plugins, tightening `AutocompleteUiState` visibility | user / default |

## Tests that move or could break

- Move to `:feature:autocomplete`:
  - **Unit:** `AutocompleteViewModelTest` (needs mockk, turbine, coroutines-test), `ResultCountTest`, `FormatResultCountTest`, `GitHubAutocompleteBarScreenshotTest` (7 goldens).
  - **androidTest:** `GitHubAutocompleteBarTest` (24), `ResourceUtilTest` (2).
- Stay in `:app`: `DetailFlowTest`, `FakeSearchModule`, `HiltTestRunner`. Only imports change.
- Risks:
  - The golden theme switch (handled above).
  - `ResourceUtilTest` reads `R.dimen.autocomplete_input_text_size`, which becomes the feature's `R`.
  - `:app`'s `check` loses its Paparazzi tests. `:app` keeps its `verifyPaparazziDebug` wiring, with no tests to verify.

## To-do

⬜ **0. Branch + plan commit** — `refactor_feature_autocomplete`. Copy this plan to
`docs/plans/feature-autocomplete-extraction.md` and commit it as `[DOCS]` first.

⬜ **1. `[CHORE]` Hilt convention plugin**
- `build-logic/convention/src/main/kotlin/AndroidHiltConventionPlugin.kt`: applies
  `com.google.devtools.ksp` + `com.google.dagger.hilt.android`; adds `implementation(hilt-android)`
  and `ksp(hilt-compiler)` via the catalog.
- Register it in `build-logic/convention/build.gradle.kts`, add a catalog alias, and add
  `hilt`/`ksp` `apply false` to the root `build.gradle.kts` if they aren't there already.

⬜ **2. `[REFACTOR]` Empty `:feature:autocomplete` module**
- `feature/autocomplete/build.gradle.kts` applies library, compose, paparazzi, hilt plugins;
  namespace `pl.gi.codingchallenge.feature.autocomplete`. Add `.gitignore`, manifest, and
  `include(":feature:autocomplete")`.

⬜ **3. `[REFACTOR]` `DemoScreen` to Compose built-ins** (before the move, so every commit builds)
- Replace `strRes`/`dimRes` with `stringResource`/`dimensionResource` in `DemoScreen.kt`, so
  `:app` no longer needs `ResourceUtil`.

⬜ **4. `[REFACTOR]` Move the autocomplete** (one atomic commit, so every commit builds)
- `git mv` the 7 main files, `testing/AutocompleteTestTags.kt`, `ResourceUtil.kt`, 4 unit tests,
  2 androidTests and 7 goldens into the module. The package becomes `feature.autocomplete` and
  `R` becomes the module's `R`.
- `AutocompleteTestTags` becomes public; `ResourceUtil` helpers become `internal`.
- Move the `autocomplete_*` entries out of `app/src/main/res/values/{strings,dimens,colors}.xml`
  into the feature's resources.
- Feature deps: `:shared`, material-icons-core, lifecycle-runtime-compose,
  hilt-navigation-compose (`hiltViewModel`), activity-compose (`BackHandler`), coroutines;
  test deps junit, mockk, turbine, coroutines-test; androidTest deps.
- `:app`: `implementation(project(":feature:autocomplete"))`; update imports in `DemoScreen` and
  `DetailFlowTest`.
- Paparazzi theme: switch to the platform theme, run `recordPaparazziDebug`, then byte-compare
  each new PNG with the old one (`cmp`). If any differs, add a minimal theme copy instead.

⬜ **5. `[AI]` Allow-rules** — `.claude/settings.json`: `./gradlew :feature:*:recordPaparazziDebug`,
`./gradlew :feature:*:verifyPaparazziDebug` (check the existing rule syntax and globbing).

⬜ **6. `[DOCS]` Docs**
- `modularization.md`: module table (`:feature:autocomplete`, `:app` thinner, `.hilt` plugin);
  exercise marked done; lessons learned (golden renames, theme, public test tags, Hilt across modules).
- `README.md`: architecture tree, Quick usage note ("lives in the app module" sentence), test
  section paths (`:feature:autocomplete:recordPaparazziDebug`).
- `kmp-knowledge-base.md` §1 and §14 map.

⬜ **7. Review**: `compose-reviewer` (sonnet) only if any UI code changed beyond package/R
imports; then `/simplify`.

⬜ **8. Self-check**
- `./gradlew ktlintCheck check` passes. Assemble all androidTests. Byte-compare the goldens.
- User runs `! ./gradlew :app:connectedDebugAndroidTest :feature:autocomplete:connectedDebugAndroidTest :feature:detail:connectedDebugAndroidTest`
  and expects 3 + 26 + 4 tests green.
- `git grep "ui.autocomplete"` finds nothing stale outside history docs. `:feature:detail` and
  `:feature:autocomplete` don't depend on each other.

## Verification

`./gradlew ktlintCheck check` · `cmp` old vs new goldens · `assembleDebugAndroidTest` for all
modules · instrumented run by the user (above).
