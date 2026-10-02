# Claude Rules

## Commit messages

- No `Co-Authored-By` or Claude/Anthropic attribution; `block-coauthor.sh` rejects it, and the
  per-session attribution reminder doesn't apply here
- Always include a bullet-point description body below the subject line — one idea per line,
  ≤72 chars, no dense paragraphs, so `git log` and blame stay easy to scan
- Don't credit where a change was decided/written (a guideline, plan, user instruction, or
  AI/Claude) — describe the technical change and reasoning only; plans and steps docs get
  deleted, so such references go stale
- Tag every subject with one bracketed tag (e.g. `[KMP] Add Ktor-based GitHubApi`). Check
  `git log --oneline -20` first — don't default to the last tag used; pick by scope below

**Tags** (8 used across this repo's history; re-derive via `git log --oneline --all | grep -oE
'\[[A-Z]+\]' | sort | uniq -c` if this ever needs re-checking). Real phase split: type tags
predate the KMP work; `[KMP]` became the umbrella once the multiplatform initiative began,
superseding type tags even for feature/fix/refactor-shaped commits; `[DOCS]` runs through both
phases. `[AI]` is a newer umbrella for AI tooling, added before its first commit, so the count
above doesn't include it yet.

| Tag | Scope |
|---|---|
| `[KMP]` | Anything in the KMP initiative: `:shared`, `iosApp`, KMP DI/networking/architecture, KMP-scoped CI (`ios-ci.yml`) |
| `[AI]` | Claude Code / AI tooling: `.claude/` settings, hooks, skills, agents, `CLAUDE.md` rules, `.mcp.json` |
| `[DOCS]` | Commits where docs are the *entire* change (a mixed doc+source commit uses the source's tag instead) |
| `[FEAT]` | New user-facing behavior, outside KMP scope |
| `[FIX]` | Bug fix, outside KMP scope |
| `[REFACTOR]` | Internal restructuring, no behavior change, outside KMP scope |
| `[TEST]` | Test-only changes, outside KMP scope |
| `[CHORE]` | Build/tooling/dependency/config housekeeping, outside KMP scope |
| `[CI]` | CI changes outside KMP scope (KMP-scoped CI uses `[KMP]`) |

## Model selection

- The main session's model is set by the user (`/model`); don't claim or switch it
- Simple/repetitive subtasks (boilerplate, formatting, renaming): delegate to a subagent with
  `model: haiku` — cheaper and faster for mechanical work
- Subagents needing real reasoning (reviews, architecture): `model: sonnet`, or `inherit` — a
  stronger model catches issues a cheap one misses
- When delegating, print `🤖 Subagent: <agent> on <model> — <why>`; this repo is also for
  learning how subagents and model tiers behave

## Context management

- Suggest `/compact` to the user when context usage nears 80% (only the user can run it)

## Clarifying questions

- Ask before starting any task; keep asking (via `AskUserQuestion`, never plain text) until 95%
  confident about requirements, so the user approves scope before any files change
- Print confidence after each round: 🔴 <70% · 🟡 70–94% · 🟢 ≥95% — don't implement below 95%
- Group related questions (up to 6/call); `multiSelect: true` for non-exclusive choices
- Exception: skip questions, confidence and the to-do list below for unambiguous single-step
  follow-ups (e.g. "tick the checkbox", "revert the test change")

## Task to-do list

- Before implementing, print a to-do list (⬜ pending · 🟡 in-progress · ✅ done · ❌ blocked),
  ending with a self-check step (review changes, verify correctness, confirm completion), so
  progress on multi-step tasks is easy to follow and misses are caught before "done"
- Update/reprint as steps complete; don't advance past a step below 95% confidence — ask if unsure
