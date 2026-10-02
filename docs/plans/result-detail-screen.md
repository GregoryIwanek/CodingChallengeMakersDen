# Plan: Result detail screen as `:feature:detail` (modularization practice)

## Context

The user wants a new feature built as its own Gradle module, as hands-on modularization
practice toward the future web+Android shop app. Chosen feature: tapping a search result opens a
full-screen **detail screen** (today a tap only shows a toast, `DemoScreen.kt:168`). It's the
shop's "product detail" pattern. It covers convention plugins, module-owned resources,
feature navigation wired by `:app`, and a feature depending on `:shared`.

## Decisions (Q&A)

| Question | Answer | Source |
|---|---|---|
| Feature | Repo/user detail screen in new `:feature:detail` | user |
| Data | Only what search returned, no new API calls; *document* "fetch full details" as a follow-up | user |
| Module | `:feature:detail` (Android-only) + `build-logic/`; `:shared` unchanged → not `[KMP]` | user/code |
| Autocomplete | Stays in `:app`; *document* extracting `:feature:autocomplete` as a follow-up | user |
| Navigation | Navigation Compose, type-safe routes; `NavHost` owned by `:app` | user |
| Route args | `DetailRoute(itemJson: String)`; `SearchResultItem` is already `@Serializable` (`SearchResultItem.kt:10`) | user/code |
| Item types | Repo and user | user |
| Avatar | Larger icon placeholder like the rows (`SearchResultRow.kt:86,112`), no Coil | user |
| Extras | Open on GitHub, Share, Copy URL (+ back arrow in top bar) | user |
| build-logic | `android-library` + `compose` convention plugins; `:app` script untouched | user |
| Resource helpers | Compose built-ins (`stringResource`/`dimensionResource`, MaterialTheme typography); *document* `:core:ui` alternative with pros/cons + recommended approach | user |
| Styling | MaterialTheme | user |
| Layout | Full-screen (no bottom bar); both Component and Overlay tabs navigate; back restores the tab + search | user |
| Repo URL | `https://github.com/$fullName` (RepoResult has no URL field); user → `htmlUrl` | code |
| New strings | `detail_*` strings; star count is a `<plurals>` | skill rule |
| Edge cases | null description → hidden; 0/1/many stars plurals; long names wrap; copy confirmation snackbar only on API < 33 (33+ shows the system one) | code/platform |
| Accessibility | Back icon contentDescription; title marked `heading()`; action buttons are icon+text with icon `contentDescription = null` | default |
| Existing goldens | Unchanged: autocomplete code isn't touched | code |
| Tests | JVM: route round-trip, URL building, Paparazzi goldens; androidTest: button callbacks, navigation (compiled, not run here) | user |
| Branch / tags | `feat_result_detail_screen` off `develop`; 5+ small commits, each asked first; no push/PR until user says | user |
| Out of scope | Network detail fetch, Coil avatars, autocomplete extraction, `:core:ui`, iOS detail screen, deep links | user |

## Existing tests the change could affect (step 3)

- `GitHubAutocompleteBarScreenshotTest` (7 goldens), `GitHubAutocompleteBarTest` (androidTest),
  `AutocompleteViewModelTest`, `ResultCountTest`, `FormatResultCountTest`, `ResourceUtilTest`:
  none reference `DemoScreen` or the toast, and the autocomplete code doesn't change, so **no expected breaks**.
- `DemoScreen` has no tests. Removing `demo_tapped_toast` and `showTappedToast` breaks nothing.
- Risk: `check` gains `:feature:detail:verifyPaparazziDebug`. The Gradle 9 HTML-report
  workaround (`app/build.gradle.kts` `reports.html.required = false`) must be repeated there.

## To-do

⬜ **0. Branch** `feat_result_detail_screen` off `develop`.

⬜ **1. `[CHORE]` build-logic convention plugins** (commit 1)
- `build-logic/settings.gradle.kts` (reads `../gradle/libs.versions.toml` as `libs`),
  `build-logic/convention/build.gradle.kts` (`kotlin-dsl`, compileOnly AGP / Kotlin compose /
  ktlint Gradle plugins; register plugin ids).
- `AndroidLibraryConventionPlugin.kt` → `codingchallenge.android.library`: `com.android.library`,
  compileSdk 37, minSdk 26, Java 11, ktlint with `android.set(true)`. Copy these values from
  `app/build.gradle.kts`.
- `AndroidComposeConventionPlugin.kt` → `codingchallenge.android.compose`: kotlin compose plugin,
  `buildFeatures.compose = true`, Compose BOM + material3 + tooling-preview deps.
- `settings.gradle.kts`: `pluginManagement { includeBuild("build-logic") }`.
- `gradle/libs.versions.toml`: add gradle-plugin library entries + the convention plugin aliases.
- Verify: `./gradlew help` and `./gradlew :app:assembleDebug` still pass. AGP 9's built-in Kotlin means
  no `kotlin-android` plugin is applied (same as `:app`); check this during implementation.

⬜ **2. `[FEAT]` Empty `:feature:detail` module** (commit 2)
- `feature/detail/build.gradle.kts` applies both convention plugins + paparazzi;
  namespace `pl.gi.codingchallenge.feature.detail`; depends on `project(":shared")`.
  Repeat the Paparazzi HTML-report workaround and the `check` → `verifyPaparazziDebug` wiring.
- `include(":feature:detail")` in `settings.gradle.kts`.
- `.github/workflows/android-ci.yml`: add `feature/detail/build/reports/{tests,paparazzi,ktlint}/`
  to the failure upload.

⬜ **3. `[FEAT]` Detail screen UI + goldens** (commit 3)
- `ResultDetailScreen.kt`: stateless `ResultDetailScreen(item, onBack, onOpenOnGitHub, onShare,
  onCopyUrl, modifier)`. Scaffold + TopAppBar (back arrow), repo body (icon, full name, owner,
  description if non-null, stars plural) / user body (icon, login, profile URL). `@Preview`s for both.
- `GitHubUrl.kt`: `SearchResultItem.gitHubUrl(): String`.
- `testing/DetailTestTags.kt` (mirrors `ui/autocomplete/testing/AutocompleteTestTags.kt`).
- `res/values/strings.xml` (`detail_*`, `<plurals name="detail_stars_count">`), `res/values/dimens.xml`.
- Tests: `GitHubUrlTest` (JVM), `ResultDetailScreenScreenshotTest` (Paparazzi: repo, repo with
  null description + 1 star, user; host theme `android:Theme.Material.Light.NoActionBar` + MaterialTheme
  wrapper, because `Theme.CodingChallenge` lives in `:app`), plus a plurals check via
  `paparazzi.context.resources`. Record goldens with `:feature:detail:recordPaparazziDebug`.
- Icons: use `material-icons-core` if it has back/share/open/copy; otherwise add small vector
  drawables to the module's `res/drawable` instead of pulling in `material-icons-extended`.

⬜ **4. `[FEAT]` Open / Share / Copy actions** (commit 4)
- `DetailActions.kt`: ACTION_VIEW intent, ACTION_SEND chooser, ClipboardManager copy +
  snackbar when API < 33. These are called from the nav entry so the screen stays stateless.
- androidTest `ResultDetailScreenTest`: back/open/share/copy invoke callbacks; repo vs user content shown.

⬜ **5. `[FEAT]` Navigation wiring** (commit 5)
- `:feature:detail`: `DetailRoute.kt` (`@Serializable data class DetailRoute(val itemJson: String)`,
  factory from `SearchResultItem`, `toItem()`), `DetailNavigation.kt`
  (`NavController.navigateToDetail(item)`, `NavGraphBuilder.detailScreen(onBack)`).
  Uses the kotlin-serialization plugin + navigation-compose.
- `:app`: `libs.androidx.navigation.compose` (check the latest stable version), `implementation(project(":feature:detail"))`;
  new `ui/AppNavHost.kt` (`DemoRoute` start → `DemoScreen(onItemClick = navController::navigateToDetail)`,
  `detailScreen(onBack = navController::popBackStack)`); `MainActivity` hosts `AppNavHost`;
  `DemoScreen` takes `onItemClick`; delete `showTappedToast` and `demo_tapped_toast`.
  The keyed `hiltViewModel`s stay scoped to the Demo back-stack entry, so search state survives the round trip.
- Tests: JVM `DetailRouteTest` (repo + user JSON round-trip); androidTest `DetailNavigationTest`
  in `:feature:detail` (test NavHost with a fake start screen → `navigateToDetail` → detail
  visible → back → start visible; no Hilt needed).

⬜ **6. `[DOCS]` Docs** (commit 6)
- `docs/architecture/modularization.md`: update the "This repo" table (`:feature:detail`,
  `build-logic/`); add a **Follow-ups** section:
  (a) fetching full details via `GET /repos`, `/users` in `:shared` (KMP/iOS impact, loading/error states);
  (b) extracting `:feature:autocomplete` (the existing exercise, now with a second feature in place to prove the no-feature-to-feature rule);
  (c) resource helpers: built-ins vs `:core:ui` vs copying, with pros/cons and the recommended approach
  (a `:core:designsystem`/`:core:ui` module once two or more modules share UI helpers or tokens, as the
  shop layout does; built-ins until then).
- `README.md` architecture tree + `docs/kmp-knowledge-base.md` module list: add `:feature:detail`, `build-logic/`.

⬜ **7. Review**: `compose-reviewer` (sonnet) on the new `:feature:detail` UI files + `AppNavHost.kt`/`DemoScreen.kt`;
then `/simplify`; fix findings (folded into the relevant commit before it's made, or a follow-up commit).

⬜ **8. Self-check**
- `./gradlew ktlintCheck check` green (unit tests, lint, both modules' Paparazzi verify).
  `assembleDebugAndroidTest` compiles the androidTests (not run, per repo rule).
- `run-android` skill: install, search, tap a repo and a user, screenshot the detail, press back,
  and confirm the search is intact.
- Re-read the diff against this table. Confirm the autocomplete goldens are byte-identical and no
  push/PR happened.

## Verification

`./gradlew ktlintCheck check` · `./gradlew :feature:detail:verifyPaparazziDebug` ·
`./gradlew :app:assembleDebugAndroidTest :feature:detail:assembleDebugAndroidTest` · emulator
smoke test via `run-android`.
