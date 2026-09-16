# KMP Knowledge Base — Field Notes from Building This

**What this is:** a topic-by-topic reference of what was actually learned building KMP into this
repo — not a step-by-step log (those guides are gone once their step shipped). This is the "look
here to refresh a topic" book. Every section follows the same shape: **core concept → what we
decided and why → real gotchas hit → where it lives in the code.**

---

## Table of contents

1. [Project anatomy & Gradle structure](#1-project-anatomy--gradle-structure)
2. [`expect`/`actual` vs. interface + DI](#2-expectactual-vs-interface--di)
3. [Dependency injection: Koin + Hilt coexistence](#3-dependency-injection-koin--hilt-coexistence)
4. [Networking: Ktor multiplatform](#4-networking-ktor-multiplatform)
5. [Serialization: kotlinx.serialization](#5-serialization-kotlinxserialization)
6. [Persistence: SQLDelight multiplatform](#6-persistence-sqldelight-multiplatform)
7. [Testing across targets](#7-testing-across-targets)
8. [Coroutines & Flow across the KMP boundary](#8-coroutines--flow-across-the-kmp-boundary)
9. [iOS interop mechanics](#9-ios-interop-mechanics)
10. [Kotlin/Native memory model](#10-kotlinnative-memory-model)
11. [CI/CD for multiplatform](#11-cicd-for-multiplatform)
12. [Master gotcha table](#12-master-gotcha-table)
13. [Still open — things to learn more about](#13-still-open--things-to-learn-more-about)
14. [Codebase map — topic to file paths](#14-codebase-map--topic-to-file-paths)

---

## 1. Project anatomy & Gradle structure

**Core concept:** a KMP module (`:shared`) declares one or more targets in `kotlin { }`; Gradle
generates a source-set hierarchy from that. `commonMain` compiles against *every* declared target;
`androidMain`/`iosMain` are per-target and can use platform APIs directly. Tests mirror this:
`commonTest` runs on every target, `androidHostTest` is Android/JVM-only.

**What we decided and why:**
- Real domain/data code lives in `commonMain`; only genuinely platform-bound code
  (`HttpClientEngine`, `SqlDriver` construction) lives in `androidMain`/`iosMain`.
- `:app` (Android) keeps Compose UI, `ViewModel`s, and the Hilt↔Koin bridge — things that are
  either UI or bound to an Android/JVM-only framework (Hilt's annotation processor, Android
  `Context`/resources). "Share logic, not UI."

**Gotchas hit:**
| Symptom | Cause | Fix |
|---|---|---|
| Declaring `iosArm64()`/`iosSimulatorArm64()` alone doesn't create `iosMain`/`iosTest` | This AGP + KMP plugin combination (`com.android.kotlin.multiplatform.library`) doesn't auto-apply Kotlin's default hierarchy template the way a plain `org.jetbrains.kotlin.multiplatform` module does | Explicit `applyDefaultHierarchyTemplate()` call at the top of `kotlin { }` |
| `androidHostTest.dependencies { }` fails to *compile the build script itself* ("Unresolved reference") | `androidHostTest` is a real source-set name but has no typed Kotlin DSL property | `getByName("androidHostTest") { dependencies { ... } }` instead |
| A backtick test name containing a comma compiles on JVM but fails Kotlin/Native ("Name contains illegal characters") | Kotlin/Native's stricter ObjC-export name-mangling | Don't use commas in backtick-quoted test names; this codebase's newer tests use plain camelCase names instead of backticks entirely to sidestep the whole class of issue |

**Where it lives:** `shared/build.gradle.kts` (`kotlin { }` block, source sets), `gradle/libs.versions.toml`.

---

## 2. `expect`/`actual` vs. interface + DI

**Core concept:** two ways to give `commonMain` code a platform-specific implementation.
`expect`/`actual` is compile-time, one declaration per target, resolved by the compiler.
Interface + DI is runtime, resolved by whatever container (Koin here) is wired up — more flexible,
more testable (fakes/mocks implement the interface directly), but more ceremony.

**What we decided and why:** `expect`/`actual` only for the one genuinely trivial case
(`Platform.platformName()`). Everything with real behavior — `GitHubSearchNetworkSource`,
`GitHubSearchCache`, `GitHubSearchRepository` — is an interface, with the platform-specific piece
pushed down to *what Koin injects into it* (`HttpClientEngine`, `SqlDriver`), not the interface
itself. Modern guidance leans interface+DI for anything non-trivial.

**Why this is a clean interview answer:** the codebase has one clear example of each, side by
side, with a stated reason for the split — not a hypothetical.

**Where it lives:** `shared/.../Platform.kt` (+ `.android.kt`/`.ios.kt`) for `expect`/`actual`;
`shared/.../domain/repository/*.kt` for the interface+DI side.

---

## 3. Dependency injection: Koin + Hilt coexistence

**Core concept:** Hilt is JVM/Android-only (annotation-processor-based, builds its graph at
compile time) — it can never run on Kotlin/Native, so it can't own `:shared`'s DI. Koin is a pure
Kotlin runtime DI framework with no annotation processing, so it works on every KMP target
including iOS.

**What we decided and why:** Koin owns everything in `:shared` (`sharedModule`, plus one
`androidPlatformModule`/`iosPlatformModule` per platform supplying the one or two genuinely
per-target pieces). Hilt keeps owning `:app`'s Android-specific graph (`ViewModel`s, Activities).
A small bridge module (`SharedKoinBridgeModule`, a Hilt `@Module` object that also implements
`KoinComponent`) is the *only* thing that knows both DI systems exist — every `@Provides` function
in it is one line: `= get()`, resolving from whatever Koin instance `startKoin { }` registered.

**Bootstrap sequencing that matters:** `CodingChallengeApp.onCreate()` calls `startKoin { }`
*before* Hilt resolves anything — Hilt's `@Provides fun ... = get()` functions would throw
`NoDefinitionFoundException` otherwise. On iOS there's no `Application.onCreate()` equivalent, so
`KoinBootstrap.kt` exposes an explicit `initKoin()` that Swift calls once at app launch.

**Where it lives:** `shared/.../di/SharedModule.kt`, `shared/src/androidMain/.../di/AndroidPlatformModule.kt`,
`shared/src/iosMain/.../di/{IosPlatformModule,KoinBootstrap,KoinHelper}.kt`,
`app/.../di/SharedKoinBridgeModule.kt`, `app/.../CodingChallengeApp.kt`.

---

## 4. Networking: Ktor multiplatform

**Core concept:** Ktor's `HttpClient` API is identical across every target; only the *engine* is
platform-specific (`OkHttp` on Android, `Darwin` on iOS) — resolved via DI so `commonMain` code
never imports anything platform-specific.

**What we decided and why — the big one this session did:** the app originally used Retrofit for
its real GitHub-search networking. Retrofit's interface-proxy model depends on JVM reflection,
which doesn't exist on Kotlin/Native — it flatly cannot run on iOS. So the real production network
layer was rewritten onto Ktor in `commonMain` (`GitHubApi`, `GitHubSearchNetworkSourceImpl`),
replacing Retrofit entirely (dropped `retrofit`, `retrofit-converter-kotlinx-serialization`,
`okhttp`, `okhttp-logging-interceptor` from `app/build.gradle.kts`). `ktor-client-okhttp` stayed —
it's now the Android *engine* Ktor itself uses, a completely different role than Retrofit played.

**Where it lives:** `shared/.../remote/GitHubApi.kt`, `shared/.../remote/GitHubSearchNetworkSourceImpl.kt`.

---

## 5. Serialization: kotlinx.serialization

**Core concept:** `@Serializable` data classes + `Json { }` — multiplatform by design, works
identically whether the wire format was consumed via Retrofit's converter (old) or Ktor's
`ContentNegotiation` (current).

**Real gotcha, the most valuable one this session hit:** the new Ktor `GitHubApi`'s
`ContentNegotiation` used the default strict `Json`, without `ignoreUnknownKeys = true`. The real
GitHub API response includes many fields the DTOs don't model (`node_id`, `private`, `language`,
`score`, ...) — strict `Json` rejected the very first real response with `Encountered an unknown
key 'node_id'`. The old Retrofit setup had `ignoreUnknownKeys = true` all along
(`NetworkModule.provideJson()`); it got dropped in the Ktor migration and **the unit tests never
caught it**, because their hand-crafted `MockEngine` fixtures only ever included fields the DTOs
actually model. Found only by testing against the real API on a live iOS run. Fixed in
`GitHubApi.kt`, and the test fixtures were deliberately strengthened afterward to include unmodeled
fields, specifically so this class of bug can't silently regress again.

**Lesson generalized:** a hand-crafted mock/fixture that only contains fields your code already
expects can never catch a "real response has more fields than modeled" bug — the test fixture has
to deliberately include noise the production API sends but your model doesn't need.

**Where it lives:** `shared/.../remote/GitHubApi.kt`, `shared/.../remote/dto/*.kt`,
`shared/src/commonTest/.../remote/GitHubApiTest.kt`.

---

## 6. Persistence: SQLDelight multiplatform

**Core concept:** SQLDelight generates typed Kotlin query APIs from `.sq` schema files, with a
platform-specific `SqlDriver` (`AndroidSqliteDriver`, `NativeSqliteDriver` on iOS, a JDBC driver
for JVM tests).

**What we decided and why:** `GitHubSearchRepository` was split into two narrower interfaces —
`GitHubSearchNetworkSource` (raw network access) and `GitHubSearchCache` (storage only) —
so `CachingGitHubSearchRepository` composes them: network-first, cache-as-fallback-on-failure (not
a TTL cache). This directly addresses a documented rate-limit gap: a failed search due to GitHub
rate-limiting returns the last-known-good result for that exact query instead of a generic error,
when one exists. Verified live on iOS (see §9) with a genuinely broken network host: the exact
same query, cached from an earlier successful run, served identical results instead of erroring.

**Real gotcha:** the JDBC sqlite driver is JVM-only — it failed to resolve for `iosArm64` the
instant that target was declared, a real Gradle dependency-resolution failure (not a compiler
error). Fixed by moving `SearchResultCacheTest.kt` out of `commonTest` into `androidHostTest`
(`git mv`'d to preserve history).

**Where it lives:** `shared/.../cache/SearchResultCache.kt`,
`shared/.../domain/repository/{GitHubSearchCache,GitHubSearchNetworkSource,GitHubSearchRepository,CachingGitHubSearchRepository}.kt`.

---

## 7. Testing across targets

**Core concept:** `commonTest` compiles and runs on every declared target — which means every
library it depends on must also support every target. `kotlin.test` is the multiplatform
test-annotation artifact (`@Test`/`assertEquals` resolve to a real per-target framework — JUnit on
Android, XCTest-backed on iOS — instead of pulling in one JVM-specific framework directly).

**The "fakes, not mocks" gotcha, and why it's not just a style preference:** MockK publishes only
a JVM variant. Kotlin resolves dependencies per-target via Gradle variant matching — Android's
target is JVM-based, so MockK resolves fine there with zero errors, which is exactly why this gap
is easy to miss until a genuine non-JVM target (`iosArm64`, `iosSimulatorArm64`) exists and Gradle
has no variant to match at all. This is a real, `no matching variant` **dependency-resolution
failure**, not a compiler error about an unresolved symbol — it happens before the Kotlin compiler
ever looks at the test source.

**What this meant in practice this session:** every MockK-based test that needed to survive the
move into `shared/commonTest` was rewritten with hand-written fake classes instead
(`CachingGitHubSearchRepositoryTest`'s `FakeNetworkSource`/`FakeCache`, `FakeGitHubSearchRepository`
for `SearchAutocompleteUseCaseTest`). One test (`GitHubSearchNetworkSourceImplTest`) was deleted
outright rather than ported — its subject no longer existed, and its coverage was redundant with
`ResultMergerTest` plus the new `GitHubApiTest`.

**Multiplatform test libraries actually used:**
| Library | Role | JVM-only equivalent it replaced |
|---|---|---|
| `kotlin.test` | `@Test`/assertions | JUnit4 (still used in `:app`'s own tests, which stay JVM-only) |
| `kotlinx-coroutines-test` | `runTest {}`, `StandardTestDispatcher`, virtual time | same, already multiplatform |
| `ktor-client-mock` (`MockEngine`) | fake HTTP layer | MockWebServer / mocking `GitHubApi` directly |
| `turbine` | asserting on `Flow` emissions | same API, just confirmed genuinely multiplatform (added to `commonTest` this session) |

**Where it lives:** `shared/src/commonTest/**`, `shared/src/androidHostTest/**` (JVM-only pieces
like the JDBC-driver test).

---

## 8. Coroutines & Flow across the KMP boundary

**Core concept:** a `suspend fun` in `commonMain` compiles to a real Kotlin/Native
Objective-C/Swift export automatically. A `Flow<T>` does *not* get the same first-class treatment
by default.

**What was verified live, not assumed — the central iOS-interop finding of this whole project:**
`SearchAutocompleteUseCase.invoke(requests: Flow<QueryRequest>): Flow<SearchOutcome>` was inspected
directly in the generated `Shared.h` header before writing any Swift against it. Findings:
- Both the `Flow` parameter and return type export as the same bare, generic
  `Kotlinx_coroutines_coreFlow` protocol — a completion-handler-based
  `collect(collector:completionHandler:)`, not `AsyncSequence`. No `for await` sugar.
- **No `MutableStateFlow` constructor was exported at all** — nothing in `iosMain` referenced one,
  so Kotlin/Native's dead-code stripping dropped it from the framework entirely.

**Decision made from that evidence:** don't drive the shared `Flow` pipeline from Swift.
`KoinHelper.kt` exposes `getGitHubSearchRepository()` (a plain `suspend fun`-returning interface)
instead of the `Flow`-based use case. `ContentView.swift` re-implements
`SearchAutocompleteUseCase`'s exact debounce (350ms) / minimum-query-length (3 chars) policy
natively, over a cancellable Swift `Task`. This duplicates one small piece of policy per platform,
in exchange for avoiding a fragile, hand-rolled `Flow`-collection bridge with no compiler-checked
element type.

**Where it lives:** `shared/.../domain/usecase/SearchAutocompleteUseCase.kt` (the shared policy,
Android's `AutocompleteViewModel` drives it directly since `ViewModel`s are Kotlin/JVM, no bridging
needed there); `iosApp/iosApp/ContentView.swift` (the Swift-native reimplementation, with a comment
pointing back at this exact finding).

---

## 9. iOS interop mechanics

**Core concept:** `:shared`'s Kotlin/Native output is an Objective-C framework, with Xcode's
"Direct Integration" (a build-phase script) as the simplest way to embed it — no CocoaPods, no
Swift Package Manager, no `Podfile`. This is genuinely the Kotlin team's current recommended
default for a project with zero CocoaPods-only dependencies.

**Mechanics that had to be verified, not guessed, each time:**
| What | How it was confirmed |
|---|---|
| The embed Gradle task's exact name | `./gradlew :shared:tasks --all \| grep -i embed` — confirmed `embedAndSignAppleFrameworkForXcode` |
| A Kotlin top-level function's Swift-visible name | Grepped the generated `Shared.framework/Headers/Shared.h` after a real embed run — file-name-derived, `Kt`-suffixed, e.g. `KoinBootstrapKt.doInitKoin()`, `KoinHelperKt.getGitHubSearchRepository()` |
| Whether `suspend fun → async` compiles as real `async`/`await` | Built and called it for real on this machine's actual Xcode/Swift toolchain, not assumed from docs |

**`@Throws` is load-bearing, not documentation — found twice, independently:** Kotlin/Native only
converts a thrown exception into a Swift-catchable `NSError` for a suspend-fun's
completion-handler bridge when the function carries a matching `@Throws` annotation. Without it,
the exception is "unexpected," and the **whole process aborts (`SIGABRT`)** instead of reaching
Swift's `catch` at all. First found on `CatFactApi.fetchCatFact()` (a real DNS failure crashed the
app until `@Throws(Exception::class)` was added). The exact same fix was needed again on
`GitHubSearchRepository.search()` once iOS started calling it directly — this project's own
history repeating the same lesson is itself worth mentioning in an interview as evidence of a
*generalizable* rule, not a one-off fix.

**Xcode toolchain mismatches are real and recur:** the local machine's Xcode version moved ahead of
what the original guide assumed mid-project (guide's assumed Xcode 15.0.1 → actual local Xcode
27.0), and later, GitHub's own hosted CI runner turned out to be capped at Xcode 26.6 — a full
major version behind the local machine, unable to even *open* a project file created with 27.0
(`objectVersion 110`, "future Xcode project file format"). See §11 for how that CI gap was handled.

**Where it lives:** `iosApp/iosApp.xcodeproj/project.pbxproj` (hand-edited directly, not via the
`xcodeproj` Ruby gem — it can't safely round-trip this project's format without corrupting it),
`shared/src/iosMain/**`, `iosApp/iosApp/ContentView.swift`.

---

## 10. Kotlin/Native memory model

Ties to a concrete thing in this codebase: Koin singletons are shared across threads with zero
`@ThreadLocal`/`freeze()` calls anywhere — the new tracing-GC memory model (default since 1.7.20,
the only option since Kotlin 2.0) is what makes that safe without extra ceremony.

---

## 11. CI/CD for multiplatform

**Core concept:** Kotlin/Native's iOS compilation is pure Gradle/Kotlin-Native — it needs a macOS
*host* (Apple's toolchain is required), but doesn't need Xcode itself for `:shared` to compile and
test. Building the actual `.xcodeproj` app shell is a separate, additional requirement that does
need Xcode.

**Real finding #1 — `./gradlew check` isn't Android-only, even on CI:** it was initially assumed
that deleting `CatFactApi` (a showcase feature) only risked breaking the Android/`:shared`-JVM
side, with the Swift/Xcode side "safely" staying broken until a later step. Wrong: `:shared`'s
`iosSimulatorArm64` target is a real Gradle-compiled target, and `KoinHelper.kt` (Kotlin code
living in `shared/src/iosMain`, not Swift) referenced `CatFactApi` directly — so `./gradlew check`
failed immediately, on every machine including CI, the moment `CatFactApi` was deleted. The fix
had to happen in the same step as the deletion, not deferred.

**Real finding #2 — GitHub Actions `paths:` filters are workflow-level, not job-level:** wanting
one expensive macOS job to only fire on iOS-relevant changes, while a cheap Android job keeps
firing on everything, requires two separate workflow files (or a `paths-filter` action + `if:`
condition) — not a `paths:` block on just one job within a shared workflow file. This repo uses
two files: `ci.yml` (Android, `ubuntu-latest`, every push/PR) and `ios-ci.yml` (`:shared`'s iOS
target, `macos-latest`, path-filtered to `shared/**` and the root Gradle files).

**Real finding #3 — the Xcode-version-skew problem, live, twice:** the first `ios-ci.yml` run
tried `xcodebuild build` for `iosApp` and failed: the runner's *default* Xcode was 26.6, the
project needs 27.0+. Adding a step to dynamically select the *newest* Xcode actually installed on
the runner didn't help either — GitHub's `macos-latest` image only had Xcode 26.0 through 26.6
installed, no 27.x at all. The fix that was **not** taken: hand-editing the project's
`objectVersion` down to something Xcode 26.6 can open — there's no Xcode 26.x install anywhere to
verify the correct target number against, and guessing risks silently corrupting the project or
dropping settings. Chose instead to scope the CI job down to `:shared:iosSimulatorArm64Test` only
(pure Gradle, no Xcode at all) and explicitly document why. **This also turns out to match how
most real KMP projects draw this exact line** — Kotlin/Native's iOS compile is verified in plain
CI; the full Xcode app-shell build is commonly left to Xcode Cloud (always on Apple's
current-release Xcode, no lag) or manual verification, specifically because this class of
hosted-runner/Xcode-version gap is a known, common pain point, not something specific to this repo.

**macOS runner cost:** GitHub-hosted macOS runners bill at a **10× minute multiplier** versus
`ubuntu-latest` — a real, concrete number worth having on hand when asked about KMP rollout costs.

**Where it lives:** `.github/workflows/ci.yml`, `.github/workflows/ios-ci.yml`.

---

## 12. Master gotcha table

Every "real, not guessable in advance" finding from this whole project, in one place — this is
the single highest-density interview-answer material in this document.

| # | Symptom | Root cause | Fix / decision |
|---|---|---|---|
| 1 | MockK silently works in `commonTest` with only an Android target, then fails once a real non-JVM target exists | MockK publishes a JVM-only variant; Android's target is JVM-based so it matches, until Gradle has to also resolve for `iosArm64`/etc. | Fakes, not mocks, in `commonTest` |
| 2 | `androidHostTest.dependencies {}` fails to compile the *build script* | No typed Kotlin DSL property for that real source-set name | `getByName("androidHostTest") { dependencies {} }` |
| 3 | Declaring iOS targets alone doesn't create `iosMain`/`iosTest` | This AGP+KMP plugin combo skips Kotlin's default hierarchy template | Explicit `applyDefaultHierarchyTemplate()` |
| 4 | JDBC sqlite driver fails to resolve the instant `iosArm64` is declared | JDBC is JVM-only, no variant for a Kotlin/Native target | Moved the JDBC-driver test out of `commonTest` into `androidHostTest` |
| 5 | `xcodeproj` Ruby gem's plain open→save silently mangled the project file | Gem doesn't safely support this Xcode project version | Hand-edited `project.pbxproj` directly, validated with `plutil -lint` |
| 6 | Gradle daemon fails writing build outputs from inside an Xcode build | `ENABLE_USER_SCRIPT_SANDBOXING = YES` (modern Xcode template default) | Set to `NO` in Debug/Release configs |
| 7 | Linker fails with `sqlite3_open_v2`/etc. "symbol(s) not found" | Direct Integration doesn't pull in `libsqlite3` transitively the way CocoaPods would | `OTHER_LDFLAGS = "-lsqlite3"` |
| 8 | A real thrown exception (DNS failure, JSON parse error) crashes the whole iOS process with `SIGABRT` instead of reaching Swift's `catch` | Missing `@Throws` on the suspend fun Swift calls directly | `@Throws(Exception::class)` — hit and fixed twice, independently (`CatFactApi`, then `GitHubSearchRepository`) |
| 9 | `./gradlew check` fails on Android/JVM CI after deleting an iOS-referenced Kotlin symbol | `:shared`'s `iosSimulatorArm64` target compiles as part of the same `check` task, everywhere, not just when targeting Xcode | Fix the iOS-side reference in the *same* step as the deletion |
| 10 | Real GitHub API response fails to parse with "unknown key" | Ktor's `ContentNegotiation` used default strict `Json`, missing `ignoreUnknownKeys = true` that the old Retrofit setup had | Added the flag; strengthened test fixtures to include unmodeled fields so it can't silently regress |
| 11 | `Flow` across the Kotlin/Native↔Swift boundary is far clunkier than a `suspend fun` | Default export is a bare completion-handler protocol, no `AsyncSequence`, no `MutableStateFlow` constructor exported (dead-code-stripped, unreferenced) | Don't bridge `Flow`; re-implement small reactive policy natively per platform instead |
| 12 | `xcodebuild` in CI: "future Xcode project file format" | GitHub's `macos-latest` runner's Xcode (26.6) is older than what the project needs (27.0+) | No safe fix without a real Xcode 26.x install to verify against — scoped CI down to `:shared` only instead of guessing |

---

## 13. Still open — things to learn more about

- **Kotlin/Native memory model** — a one-sentence answer exists, but articulating it live,
  unprompted, still needs rehearsal — nothing in the code *forces* you to explain it.
- **Compose Multiplatform** — never explored; this project deliberately went all-native (Compose
  Android + SwiftUI iOS). Know *when* a team would reach for it instead, even without having built
  with it.
- **Napier** (multiplatform logging) and **multiplatform-settings** (key-value storage) — named in
  the original gap table, never came up because the project never needed them.
- **A second memorized migration case study** — one real-world case is covered in depth; having a
  second one memorized would round this out.
- **Production-scale competency** — explicitly *not* closeable by any amount of solo prep. Frame
  honestly in an interview rather than overstating it.
- **Non-KMP gaps** (mobile security/auth, SAFe, etc.) — entirely out of scope for this document.

---

## 14. Codebase map — topic to file paths

A quick-lookup table for "where do I go to refresh my memory on X by re-reading real code."

| Topic | Path |
|---|---|
| Gradle/source-set structure | `shared/build.gradle.kts` |
| `expect`/`actual` | `shared/src/commonMain/.../Platform.kt` + `androidMain`/`iosMain` variants |
| Koin DI setup | `shared/.../di/SharedModule.kt` |
| Hilt↔Koin bridge | `app/.../di/SharedKoinBridgeModule.kt` |
| iOS Koin bootstrap | `shared/src/iosMain/.../di/{KoinBootstrap,KoinHelper}.kt` |
| Ktor networking | `shared/.../remote/GitHubApi.kt`, `GitHubSearchNetworkSourceImpl.kt` |
| Serialization DTOs | `shared/.../remote/dto/*.kt` |
| SQLDelight cache | `shared/.../cache/SearchResultCache.kt` |
| Network-first/cache-fallback repository | `shared/.../domain/repository/CachingGitHubSearchRepository.kt` |
| Debounce/search policy (Android) | `shared/.../domain/usecase/SearchAutocompleteUseCase.kt` |
| Debounce/search policy (iOS, re-implemented) | `iosApp/iosApp/ContentView.swift` |
| Fakes-not-mocks test examples | `shared/src/commonTest/.../domain/repository/{CachingGitHubSearchRepositoryTest,FakeGitHubSearchRepository}.kt` |
| `MockEngine` HTTP test example | `shared/src/commonTest/.../remote/GitHubApiTest.kt` |
| Android CI | `.github/workflows/ci.yml` |
| iOS CI | `.github/workflows/ios-ci.yml` |
