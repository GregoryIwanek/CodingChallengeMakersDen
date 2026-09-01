# GitHub Users & Repositories Autocomplete

A reusable Jetpack Compose component that searches GitHub users and
repositories as you type, merges both result types into one
alphabetically-sorted list, and floats over whatever screen hosts it —
built from scratch, no autocomplete library.

## Demo

| Overview | Component | Overlay |
|---|---|---|
| ![Overview tab](docs/screenshots/overview.png) | ![Component tab](docs/screenshots/component.png) | ![Overlay tab](docs/screenshots/overlay.png) |

The demo app is three tabs: **Overview** describes the assignment and
its requirements, **Component** hosts the bar with the panel pushing
the rest of the screen down, and **Overlay** hosts the exact same bar
floating over an unrelated scrolling feed instead — the same component
in both of its layout modes, side by side.

## Quick usage

Drop `GitHubAutocompleteBarComponent` onto any screen inside a `Box` —
it owns only the bar and the suggestion panel below it, never the
backdrop, scrim, or navigation. This is a take-home assignment, so the
component lives directly in the app module; in a real project it
would ship as its own Gradle module, or a published library (AAR via
Maven/JitPack) if it needed to be shared across separate apps or
repos:

```kotlin
Box(Modifier.fillMaxSize()) {
    HostScreenContent()
    GitHubAutocompleteBarComponent(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(16.dp)
            .fillMaxWidth(),
        onItemClick = { item -> navigateToDetail(item) },
    )
}
```

That's it — debouncing, networking, and state rendering are all
handled internally via Hilt. See `DemoScreen.kt` for the three-tab
example above: Component and Overlay both drop in this same
composable, each with its own `AutocompleteViewModel` instance so
typing in one tab never leaks into the other.

## Requirements checklist

| Requirement | Where it lives |
|---|---|
| Minimum 3 characters before searching | `SearchAutocompleteUseCase`'s `MIN_QUERY_LENGTH` gate |
| Fetches both users and repositories | `GitHubSearchRepositoryImpl`, launched in parallel via `async` |
| Combined, alphabetically-sorted list | `ResultMerger.mergeAndSort`, keyed on repository name / login |
| Capped at 50 results | `SearchLimits.MAX_RESULTS` — single source of truth for both the per-type fetch limit and the final cap |
| Loading / empty / error states | `AutocompleteUiState`, rendered by `SuggestionPanel` |
| Rapid-input handling | `debounce` + `distinctUntilChanged` + `flatMapLatest` in the use case |
| Reusable, not hardcoded to one screen | `GitHubAutocompleteBarComponent` — plain composable API, demoed over three different screens |
| No autocomplete library | Search bar, dropdown, and every state are hand-built Compose |

## Technologies used

- **Kotlin** 2.3.21, Android SDK 37 (`minSdk` 26), AGP 9.3.2
- **Jetpack Compose** (BOM `2026.08.00`) + Material3 — the entire UI,
  no XML layouts
- **Hilt** 2.60.1 (via KSP) — dependency injection, including a
  per-tab-keyed `hiltViewModel()` in the demo
- **Retrofit** 3.0.0 + **OkHttp** 4.12.0 + **kotlinx.serialization**
  1.11.0 — networking and JSON parsing, no Gson/Moshi
- **Kotlin Coroutines** 1.11.0 — `debounce`/`flatMapLatest` in the use
  case, parallel user/repo fetch via `async` in the repository
- **Testing** — JUnit4, [MockK](https://mockk.io), and
  [Turbine](https://github.com/cashapp/turbine) (Flow testing) for
  unit tests; Espresso + Compose UI Test for instrumented tests;
  [Paparazzi](https://github.com/cashapp/paparazzi) for JVM
  screenshot tests (see Testing below)

## Architecture

Clean Architecture + MVVM. Dependencies point inward: `ui/` and
`data/` both depend on `domain/`, never the other way around.

```
pl.gi.codingchallenge
├── di/                                  # Hilt modules
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
├── domain/                              # plain Kotlin, no Android/Compose imports
│   ├── model/
│   │   ├── SearchResultItem.kt          # sealed interface: UserResult / RepoResult
│   │   ├── SearchOutcome.kt             # sealed interface: the use case's output
│   │   ├── QueryRequest.kt              # query text + retry counter
│   │   └── SearchLimits.kt              # MAX_RESULTS
│   ├── usecase/
│   │   └── SearchAutocompleteUseCase.kt # debounce, min-length gate, cancel-on-new-input
│   ├── repository/
│   │   └── GitHubSearchRepository.kt    # interface only
│   └── ResultMerger.kt                  # merges + sorts + caps both result types
├── data/                                # the only layer that knows about Retrofit/GitHub's REST shape
│   ├── remote/
│   │   ├── GitHubApi.kt                 # Retrofit interface
│   │   └── dto/                         # UserDto, RepositoryDto, GitHubSearchResponse<T>
│   ├── model/
│   │   └── ResultMappers.kt             # DTO -> domain
│   └── repository/
│       └── GitHubSearchRepositoryImpl.kt  # parallel fetch via async; implements domain/repository
├── ui/
│   ├── autocomplete/                    # the reusable component
│   │   ├── AutocompleteViewModel.kt     # maps SearchOutcome -> AutocompleteUiState
│   │   ├── AutocompleteUiState.kt       # Idle/Loading/Success/Empty/Error
│   │   ├── GitHubAutocompleteBar.kt     # public entry point (Hilt-backed + stateless overload)
│   │   ├── FloatingSearchBar.kt         # the pill bar itself
│   │   ├── SuggestionPanel.kt           # the floating results card
│   │   ├── SearchResultRow.kt           # item renderer
│   │   └── testing/AutocompleteTestTags.kt
│   └── DemoScreen.kt                    # 3-tab demo host — see Demo above
├── util/
│   └── ResourceUtil.kt                  # dimRes/strRes/colRes/spRes helpers
├── CodingChallengeApp.kt                # @HiltAndroidApp
└── MainActivity.kt
```

- `SearchAutocompleteUseCase` returns a domain-only `SearchOutcome`
  (`QueryTooShort` / `Loading` / `Success` / `Failure`) — deliberately
  no `Empty` variant, since an empty result set is just
  `Success(emptyList())`; deciding to render that as an "empty state"
  is a UI concern, left to `AutocompleteViewModel`.
- `GitHubSearchRepositoryImpl` implements the `domain/repository`
  interface (dependency inversion) rather than `domain/` depending on
  `data/`.

## Testing

27 unit tests + 22 instrumented tests across 9 files:

- **`SearchAutocompleteUseCaseTest`** (6) — debounce, cancellation,
  the min-length gate, and retry, all driven by a virtual-time test
  dispatcher, no real delays.
- **`GitHubSerializationTest`** (2) — decodes real (trimmed) GitHub
  JSON payloads through the actual `kotlinx.serialization` pipeline,
  not just hand-built DTOs, so a wrong `@SerialName` would actually
  fail a test.
- **`GitHubAutocompleteBarTest`** (20, instrumented) — every UI state,
  focus handling, back-press, scroll-reset on a new query, and the
  divider/leading-icon edge cases, via real Compose UI interactions.
- **`GitHubAutocompleteBarScreenshotTest`** (5) — [Paparazzi](https://github.com/cashapp/paparazzi)
  golden-image tests, one per UI state. Unlike the assertion-based
  tests above, which only check semantics (does this tag exist, does
  this text say X), each of these renders the composable to a bitmap
  and diffs it pixel-for-pixel against a checked-in reference image
  (the "golden"). That's what catches a purely visual regression —
  wrong spacing, a clipped row, a color that quietly changed — that
  would pass every semantic assertion untouched. Paparazzi renders on
  the JVM via a bundled Android SDK, so this runs in plain unit tests
  with no emulator; a failure produces a diff image showing exactly
  what changed. `./gradlew :app:recordPaparazziDebug` (re)generates
  the goldens after an intentional UI change;
  `:app:verifyPaparazziDebug` (part of `check`) is what actually fails
  the build on a mismatch.
- Plus `ResultMergerTest`, `ResultMappersTest`,
  `GitHubSearchRepositoryImplTest`, `AutocompleteViewModelTest`, and
  `ResourceUtilTest` (instrumented) covering the remaining
  domain/data/presentation logic.

Run everything: `./gradlew check` (unit tests + lint + Paparazzi) and
`./gradlew connectedDebugAndroidTest` (needs a device/emulator). CI
runs `./gradlew check` on every push and pull request to `main` and
`develop` — see `.github/workflows/ci.yml`.

## Running it

1. Clone the repo and open it in Android Studio.
2. Sync Gradle, then run the `app` configuration on a device or
   emulator.
3. `./gradlew check` runs the full non-instrumented test suite.

No API token is required — this uses GitHub's public, unauthenticated
search endpoints.

## Known limitations

- GitHub's unauthenticated search API is capped at 10 requests/minute
  per IP; hitting it mid-session falls into the same generic error
  state as any other failure, with no differentiated "rate limited,
  try again shortly" state.

## Development notes

This project was built with the help of AI coding assistants —
primarily [Claude Code](https://claude.com/claude-code), with
[OpenCode](https://opencode.ai) also used for part of the work —
alongside manual review, on-device testing, and the architectural
decisions described above.

## License

MIT
