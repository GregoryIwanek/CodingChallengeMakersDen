# Hook Upgrades: Optional Steps

The optional upgrades from the deleted hook walkthroughs, kept for later.

## ktlint-on-edit (PostToolUse)

- **Speed:** each edit formats the whole project. Map the path to a module, so a file under
  `shared/` runs `:shared:ktlintFormat` and one under `app/` runs `:app:ktlintFormat`.
- **Feedback:** replace `|| true` with code that sends the output to stderr and exits with 2.
  Claude then sees violations ktlint couldn't auto-fix and fixes them itself.

## block-coauthor (PreToolUse)

- **Block other risky commands** with the same pattern: `git push --force`, `git reset --hard`,
  `rm -rf`. Each is one more `if` block, or a separate script under the same matcher.
- **Cover your own commits too:** a `commit-msg` git hook (e.g. in `.githooks/`, enabled with
  `git config core.hooksPath .githooks`) rejects the trailer whoever commits.
- **Trim `CLAUDE.md`:** once the hook works, shorten the Co-Authored-By rule to one line saying it
  is enforced by `block-coauthor.sh`. That's the last checklist step, so it can wait.

Known gaps worth fixing alongside:

- `git commit -F msg.txt` with the trailer inside the file gets through; the hook only reads the
  command text.
- The hook matches `co-authored-by` anywhere in a commit command, so a commit message that merely
  mentions the trailer is blocked too. Reword it (e.g. "co-author trailer").

## compile-check (Stop)

- **Re-check the fix:** instead of skipping when `stop_hook_active` is true, count attempts (e.g.
  in a temp file keyed by `session_id`) and give up after 2–3, so Claude's fix is verified too.
- **Faster check:** compile only the module that changed: `:app:compileDebugKotlin` for `app/`,
  `:shared:compileAndroidMain` for `shared/`.
- **Cover iOS-only code:** when a file under `shared/src/iosMain/` changed, also run
  `:shared:compileKotlinIosSimulatorArm64`. Much slower, so only for that path.
- **Unit tests on demand:** a second Stop step that runs `testDebugUnitTest` only when a test
  file changed.

Known gaps worth fixing alongside:

- The skip check reads the whole working tree, not the turn. Any uncommitted `.kt`/`.kts` change
  makes every later turn compile, even turns that touched no files.
- Exit 2 only feeds the error back to Claude; it doesn't force a fix. An explicit "don't fix it"
  instruction can win, so the hook is a nudge, not a guarantee.
