# Claude Rules

## Commit messages

- Never add `Co-Authored-By` or Claude/Anthropic attribution
- Always include a bullet-point description body below the subject line — one idea per line,
  ≤72 chars, no dense paragraphs
- Don't credit where a change was decided/written (a guideline, plan, user instruction, or
  AI/Claude) — describe the technical change and reasoning only
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

- Simple/repetitive tasks (boilerplate, formatting, renaming): spawn a subagent using
  `claude-haiku-4-5-20251001`
- Complex reasoning, architecture, multi-step tasks: use `claude-sonnet-4-6` (default)
- Print the rating once confidence is reached and implementation starts:
  `🤖 Model: Haiku 4.5 (subagent) — rated as simple/repetitive` /
  `🤖 Model: Sonnet 4.6 — rated as complex/multi-step`

## Context management

- Run `/compact` at 80% context usage

## Clarifying questions

- Ask before starting any task; keep asking (via `AskUserQuestion`, never plain text) until 95%
  confident about requirements
- Print confidence after each round: 🔴 <70% · 🟡 70–94% · 🟢 ≥95% — don't implement below 95%
- Group related questions (up to 6/call); `multiSelect: true` for non-exclusive choices

## Task to-do list

- Before implementing, print a to-do list (⬜ pending · 🟡 in-progress · ✅ done · ❌ blocked),
  ending with a self-check step (review changes, verify correctness, confirm completion)
- Update/reprint as steps complete; don't advance past a step below 95% confidence — ask if unsure
