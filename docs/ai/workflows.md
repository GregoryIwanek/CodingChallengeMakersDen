# Claude Code Workflows

How to use this repo's Claude Code harness for everyday work: one recipe per kind of task, each a
short chain of steps saying who does what and which harness piece fires.

Setup and first-time install: [getting-started.md](getting-started.md). Concepts:
[claude-code-harness.md](claude-code-harness.md).

**Legend:** 🧑 you · 🤖 Claude · `(hook)` the harness piece that fires on that step

| # | Workflow | Use it when |
| --- | --- | --- |
| 1 | [Add a new feature](#1-add-a-new-feature) | New user-facing behavior, design choices to make |
| 2 | [Small change to existing code](#2-small-change-to-existing-code) | A tweak with an obvious spec: copy, padding, rename |
| 3 | [Fix a bug](#3-fix-a-bug) | A backlog ticket or a reported defect |
| 4 | [Change `:shared` (KMP)](#4-change-shared-kmp) | Domain, networking or cache code shared with iOS |
| 5 | [Prepare guidelines](#5-prepare-guidelines) | A new rule, convention or design decision |
| 6 | [Add a harness piece](#6-add-a-harness-piece) | A hook, skill, subagent or permission rule |
| 7 | [Review a branch or PR](#7-review-a-branch-or-pr) | Before merging anything non-trivial |
| 8 | [CI failed](#8-ci-failed) | A red check on a PR |
| 9 | [Dependency or build chore](#9-dependency-or-build-chore) | Library, AGP or Kotlin bumps |
| 10 | [Release `develop` → `main`](#10-release-develop--main) | Making `main` match `develop` |
| 11 | [Learn or explain code](#11-learn-or-explain-code) | Questions, no changes |
| 12 | [Session hygiene](#12-session-hygiene) | Every session |

---

## The setup at a glance

Everything lives in `.claude/` (committed) and loads when a session starts.

| Layer | Piece | What it does |
| --- | --- | --- |
| **Rules** | `.claude/CLAUDE.md` | Ask until 95% sure, print a to-do list, commit tags, model tiers |
| | Auto-memory | Personal preferences: named args, ask before every commit, no doc refs in code |
| **Guards** | `settings.json` → `allow` | Gradle build/test/Paparazzi, `adb`, read-only `git` and `gh` |
| | `settings.json` → `deny` | `connected*AndroidTest`, reading `local.properties` |
| **Hooks** | `SessionStart` | Loads open tickets from `docs/backlog.md` into context |
| | `PreToolUse` (Bash) | `block-coauthor.sh`, `block-remote-delete.sh` |
| | `PostToolUse` (Edit/Write) | `ktlint-on-edit.sh`: formats `.kt` after every edit |
| | `Stop` | `compile-check.sh`: Claude can't finish while Kotlin doesn't compile |
| **Skills** | `/new-feature` | Questions → affected tests → plan, no edits |
| | `/run-android` | Install, launch, screenshot, look |
| | Built in | `/code-review`, `/simplify`, `/security-review` |
| **Agents** | `compose-reviewer` | Read-only Compose review (Read, Grep, Glob) |
| **CI** | `android-ci.yml` | `ktlintCheck` → `check` (unit tests, lint, Paparazzi verify) |
| | `ios-ci.yml` | `:shared` tests on iOS, only when `:shared` changes |

---

## 1. Add a new feature

> **Chain:** branch → `/new-feature` → answer questions → approve plan → implement → tests and
> goldens → device check → review → commit → PR → merge

| Step | Who | What | Fires |
| --- | --- | --- | --- |
| 1 | 🧑 | `git checkout develop && git pull`, then `claude` | `SessionStart` loads the backlog |
| 2 | 🧑 | `/new-feature <description>` | |
| 3 | 🤖 | Reads the code and finds existing tests the change could break | |
| 4 | 🧑 | Answers the question rounds until 🟢 95% | `CLAUDE.md` rules |
| 5 | 🤖 | Shows the answers table and a to-do plan; 🧑 approves | |
| 6 | 🤖 | Creates `feat_<name>` off `develop` and implements | ktlint per edit, compile check at stop |
| 7 | 🤖 | Unit tests + `recordPaparazziDebug`; looks at every changed golden, reverts noise-only ones | `allow` rules |
| 8 | 🤖 | `/run-android`; 🧑 does the taps it can't guess | |
| 9 | 🤖 | `compose-reviewer`, then `/simplify`; applies or skips each finding | |
| 10 | 🧑 | Runs `connectedDebugAndroidTest` (denied for Claude) | `deny` rule |
| 11 | 🤖 | Asks before committing, then `[FEAT]` commit | no Co-Authored-By (hook) |
| 12 | 🤖 | Pushes and opens a PR to `develop`; 🧑 merges; 🤖 pulls and deletes the local branch | CI |

> **Lesson from the first run:** the biggest catch came from reading the existing tests before
> planning, not from the questions. Keep step 3.

## 2. Small change to existing code

> **Chain:** branch → plain prompt → edit → affected tests → commit → PR

| Step | Who | What | Fires |
| --- | --- | --- | --- |
| 1 | 🧑 | Branch off `develop`, or ask Claude to | |
| 2 | 🧑 | Plain prompt with what and where: *"header padding → 12dp"* | |
| 3 | 🤖 | Skips question rounds (single-step follow-up exception) | `CLAUDE.md` |
| 4 | 🤖 | Edits and runs the affected unit tests | ktlint |
| 5 | 🤖 | UI touched: `verifyPaparazziDebug`; re-records only intended diffs and views the PNG | |
| 6 | 🤖 | Asks before committing; `[FIX]` / `[REFACTOR]` / `[FEAT]` → push → PR | co-author hook |
| 7 | 🧑 | Merges; 🤖 pulls | |

## 3. Fix a bug

> **Chain:** ticket → failing test → root-cause fix → review → commit → PR → resolve ticket

| Step | Who | What | Fires |
| --- | --- | --- | --- |
| 1 | 🧑 | *"Fix &lt;ticket&gt;"*: open tickets are already in context | `SessionStart` |
| 2 | 🤖 | Reproduces it with a failing unit test, or says why it can't | |
| 3 | 🤖 | Fixes the root cause, not the symptom; the test goes green | |
| 4 | 🤖 | `/code-review` on the branch for side effects | |
| 5 | 🤖 | `[FIX]` commit (`[KMP]` if in `:shared`) → PR | |
| 6 | 🤖 | After merge, marks the ticket Resolved in `docs/backlog.md` | memory rule |

## 4. Change `:shared` (KMP)

> **Chain:** `/new-feature` → `[KMP]` plan → shared tests on both targets → check Swift → PR with
> both CIs

| Step | Who | What | Fires |
| --- | --- | --- | --- |
| 1 | 🧑 | `/new-feature …` and answer Q4 with `:shared` | |
| 2 | 🤖 | Tag becomes `[KMP]`; the plan covers iOS impact | |
| 3 | 🤖 | `:shared:allTests`, plus `:shared:iosSimulatorArm64Test` on macOS | `allow` rules |
| 4 | 🤖 | Checks `iosApp` still compiles and mirrors any shared policy | |
| 5 | 🤖 | PR; `android-ci` **and** `ios-ci` run; 🧑 merges | both CIs |

## 5. Prepare guidelines

> **Chain:** rule + reason → find existing → pick the right home → write short → commit → verify
> in a new session

| Step | Who | What |
| --- | --- | --- |
| 1 | 🧑 | Decides the rule **and why**; rules without a reason drift |
| 2 | 🤖 | Checks for an existing rule or memory to update instead of duplicating |
| 3 | 🤖 | Picks where it goes (table below) |
| 4 | 🤖 | Writes it short: one-line rule + "so that…" |
| 5 | 🤖 | `[AI]` (harness) or `[DOCS]` commit → PR |
| 6 | 🧑 | New session; confirms the rule is followed |

| The rule is… | It goes in |
| --- | --- |
| A team rule, always on | `.claude/CLAUDE.md` |
| A personal preference | Auto-memory (`feedback_*.md`) |
| Mechanically checkable | A hook or a `deny` rule, not prose |
| A design decision | `docs/architecture/<decision>.md` |

## 6. Add a harness piece

> **Chain:** describe behavior → pick the tool → write + register → test both ways → commit →
> restart session

| Step | Who | What |
| --- | --- | --- |
| 1 | 🧑 | Describes the behavior to enforce or automate |
| 2 | 🤖 | Picks the tool (table below) |
| 3 | 🤖 | Writes the file, `chmod +x` for scripts, registers it in `settings.json` |
| 4 | 🤖 | Tests the script directly with should-block **and** should-allow cases |
| 5 | 🤖 | `[AI]` commit → PR; 🧑 merges |
| 6 | 🧑 | Starts a **new** session (settings load at start); checks `/hooks` or `/agents` |

| Tool | Best for | Watch out |
| --- | --- | --- |
| `deny` rule | Blocking one command by prefix | Misses reordered flags (`git push --delete origin x`) |
| Hook | Any check on the full command or edit | Only sees command text, not scripts it calls |
| Skill | A repeatable multi-step prompt | Only runs when invoked |
| Subagent | Isolated work with restricted tools | Starts cold; pass it file paths |

## 7. Review a branch or PR

> **Chain:** checkout → `/code-review` → `/simplify` → `compose-reviewer` → (security) → decide

| Step | Who | What |
| --- | --- | --- |
| 1 | 🧑 | Checks out the branch, or passes a PR number |
| 2 | 🤖 | `/code-review` for bugs |
| 3 | 🤖 | `/simplify` for reuse, simplification and efficiency |
| 4 | 🤖 | `compose-reviewer` on changed composables |
| 5 | 🤖 | `/security-review` if networking, auth or storage changed |
| 6 | 🧑 | Decides which findings to apply → commit |

## 8. CI failed

> **Chain:** report → read the run → reproduce locally → fix → push

| Step | Who | What | Fires |
| --- | --- | --- | --- |
| 1 | 🧑 | *"CI failed on PR #N"* | |
| 2 | 🤖 | `gh pr checks` / `gh run view` | read-only `gh` allowed |
| 3 | 🤖 | Reproduces with `ktlintCheck`, `check` or `verifyPaparazziDebug` | |
| 4 | 🤖 | Fixes in a **new** commit (no amend once pushed) → push → re-checks | |

## 9. Dependency or build chore

> **Chain:** bump → read release notes → full checks → judge golden diffs → commit

| Step | Who | What |
| --- | --- | --- |
| 1 | 🧑 | *"Bump &lt;lib&gt;"* or *"upgrade AGP/Kotlin"* |
| 2 | 🤖 | Edits `gradle/libs.versions.toml` and reads the release notes |
| 3 | 🤖 | `./gradlew check` + `:shared:allTests` |
| 4 | 🤖 | Paparazzi diffs: compares pixels to tell rendering noise from real change |
| 5 | 🤖 | `[CHORE]` (or `[KMP]`) commit → PR |

## 10. Release `develop` → `main`

> **Chain:** list what ships → PR → resolve `.gitignore` → merge → pull

| Step | Who | What |
| --- | --- | --- |
| 1 | 🤖 | `git rev-list --left-right --count origin/main...origin/develop` |
| 2 | 🤖 | PR `develop` → `main`; resolves `.gitignore` so `.claude/` stays tracked |
| 3 | 🧑 | Reviews and merges on GitHub |
| 4 | 🤖 | Pulls `main`; README and screenshots are current on the default branch |

> ⚠️ `main` still has `[CHORE] Ignore .claude and .idea directories`, which `develop` reversed.
> Resolve that conflict in favor of `develop`, or `main` stops tracking the harness.

## 11. Learn or explain code

> **Chain:** question → read → answer with `file:line` → keep what lasts

| Step | Who | What |
| --- | --- | --- |
| 1 | 🧑 | *"Explain how search debounce works"* / *"trace a query end to end"* |
| 2 | 🤖 | Reads the code (an Explore agent only for broad sweeps) |
| 3 | 🤖 | Answers with `file:line` references, a diagram if it helps |
| 4 | 🧑 | Moves anything worth keeping into `docs/kmp-knowledge-base.md` |

## 12. Session hygiene

| When | Do |
| --- | --- |
| Starting work | Check the status line: branch and context % |
| Context near 80% | 🧑 `/compact` |
| After changing settings or hooks | Restart the session |
| After a merge | Pull, delete the local branch |
| Remote branches to clean up | 🧑 Delete them yourself; `block-remote-delete.sh` blocks Claude |
| Instrumented tests | 🧑 Run `connectedDebugAndroidTest` yourself; it's denied for Claude |

---

## Known gaps

| Gap | Effect | Fix |
| --- | --- | --- |
| `main` ignores `.claude/` | A plain `develop` → `main` merge can drop the harness | See [workflow 10](#10-release-develop--main) |
