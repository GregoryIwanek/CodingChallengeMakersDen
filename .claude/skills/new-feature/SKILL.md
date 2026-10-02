---
name: new-feature
description: Start a new feature plan-first. Answers the standard feature questions from the
  code, asks the user only the real trade-offs, then proposes a plan without editing any files.
disable-model-invocation: true
argument-hint: <feature description>
---

# Start a new feature: $ARGUMENTS

Plan only. Don't edit, create or delete files until the user approves the plan.

Before anything else, call `EnterPlanMode` (skip if plan mode is already on), so edits are
blocked by the harness, not just by this rule. Write the plan to the plan file it gives you.

1. If `$ARGUMENTS` is empty, ask what the feature is and stop.
2. Read the code the feature touches, plus any spec under `docs/` that it names.
3. Find the existing tests covering that code: unit, Paparazzi and androidTest. List each
   assertion the change could break, e.g. a fixed height or an exact string. androidTests can't
   run here, so a break there goes unnoticed unless you find it now.
4. Go through the questions below.
   - Answer everything `$ARGUMENTS` or the code settles yourself, citing the file. Don't ask it.
   - Ask the user only real trade-offs, with `AskUserQuestion`, grouped, following the
     clarifying-questions rules in `CLAUDE.md`.
   - Default to the simplest version that delivers the feature. Put that first as
     "(Recommended)". Each extra option says what it adds: files, API surface, tests.
   - Use `multiSelect` only for independent opt-in extras. Never put a "none" option next to
     extras in the same multi-select.
   - If answers contradict each other, ask again rather than picking a reading.

   1. What exactly does the user see, and in which UI states?
   2. What are the edge cases?
   3. Which data does it show, and where does it come from?
   4. Which module: `:app` only, or `:shared`? `:shared` means iOS too, the `[KMP]` tag and iOS CI.
   5. Which files and existing patterns should it follow? Reuse existing dimens, strings and
      `util/` helpers before adding new ones.
   6. New strings? Counts need `<plurals>`, not `<string>`.
   7. Accessibility: anything beyond plain, in-order text?
   8. Which tests prove it: unit tests, Paparazzi screenshots? A JVM test can check real
      resource strings through `paparazzi.context.resources`, with no Robolectric needed.
   9. Does it change existing Paparazzi goldens? `check` fails on any pixel change.
   10. Commit tag and branch name (`feat_<name>` off `develop`)?
   11. What's out of scope?
5. Show the answers as a short table: question, answer, source (args, code or user).
6. Propose a plan as a to-do list naming the files to change, new resources, the tests to add,
   any Paparazzi re-record and the step-3 tests the change affects. End with a review step
   (`compose-reviewer` for UI, then `/simplify`) and a self-check step. Then call
   `ExitPlanMode` so the user approves it in the plan dialog.
