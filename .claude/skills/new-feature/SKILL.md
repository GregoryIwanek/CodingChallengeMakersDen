---
name: new-feature
description: Start a new feature plan-first. Answers the standard feature questions from the
  code, asks the user the rest, then proposes a plan without editing any files.
disable-model-invocation: true
argument-hint: <feature description>
---

# Start a new feature: $ARGUMENTS

Plan only. Don't edit, create or delete files until the user approves the plan.

1. If `$ARGUMENTS` is empty, ask what the feature is and stop.
2. Read the code the feature touches, plus any spec under `docs/` that it names.
3. Go through the questions below. Answer what `$ARGUMENTS` or the code settles, citing the
   file for code answers. Ask the user the rest with `AskUserQuestion`, grouped, following the
   clarifying-questions rules in `CLAUDE.md`.
   1. What exactly does the user see, and in which UI states?
   2. What are the edge cases?
   3. Which data does it show, and where does it come from?
   4. Which module: `:app` only, or `:shared`? `:shared` means iOS too, the `[KMP]` tag and iOS CI.
   5. Which files and existing patterns should it follow?
   6. New strings? Counts need `<plurals>`, not `<string>`.
   7. Accessibility: anything beyond plain, in-order text?
   8. Which tests prove it: unit tests, Paparazzi screenshots?
   9. Does it change existing Paparazzi goldens? `check` fails on any pixel change.
   10. Commit tag and branch name (`feat_<name>` off `develop`)?
   11. What's out of scope?
4. Show the answers as a short table: question, answer, source (args, code or user).
5. Propose a plan as a to-do list naming the files to change, new resources, the tests to add
   and any Paparazzi re-record, ending with a self-check step. Wait for approval.
