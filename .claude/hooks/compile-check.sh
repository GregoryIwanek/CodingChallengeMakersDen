#!/usr/bin/env bash
input=$(cat)
[[ $(jq -r '.stop_hook_active // false' <<<"$input") == "true" ]] && exit 0
cd "$CLAUDE_PROJECT_DIR" || exit 0
git status --porcelain -uall | grep -qE '\.kts?$' || exit 0
if ! out=$(./gradlew compileDebugKotlin -q --console=plain 2>&1); then
  errors=$(grep -E '^e: ' <<<"$out" | head -n 20)
  echo "Compilation failed. Fix these errors before finishing:" >&2
  echo "${errors:-$(tail -n 20 <<<"$out")}" >&2
  exit 2
fi
exit 0
