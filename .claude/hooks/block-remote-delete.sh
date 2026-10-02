#!/usr/bin/env bash
# Blocks every way of deleting a remote branch or tag; the user deletes those by hand.
cmd=$(jq -r '.tool_input.command // empty')

block() {
  echo "Blocked: deleting remote branches or tags is not allowed from Claude ($1). Ask the user to delete it on GitHub or in their own terminal." >&2
  exit 2
}

# One command per line, so `cd x && git push ...` is checked per command and a
# commit message mentioning `git push --delete` mid-line isn't.
segments=${cmd//&&/$'\n'}
segments=${segments//||/$'\n'}
segments=${segments//;/$'\n'}
segments=${segments//|/$'\n'}

while IFS= read -r seg; do
  if [[ $seg =~ ^[[:space:]]*git([[:space:]]+-C[[:space:]]+[^[:space:]]+)?[[:space:]]+push([[:space:]]|$) ]]; then
    args=${seg#*push}
    grep -qE '(^|[[:space:]])(--delete|-d|--prune|--mirror)([[:space:]]|=|$)' <<<"$args" && block "git push delete flag"
    grep -qE '(^|[[:space:]])\+?:[^[:space:]]' <<<"$args" && block "git push :refspec"
  elif [[ $seg =~ ^[[:space:]]*gh[[:space:]]+pr[[:space:]]+merge([[:space:]]|$) ]]; then
    grep -qE '(^|[[:space:]])(--delete-branch(=true)?|-d)([[:space:]]|$)' <<<"$seg" && block "gh pr merge --delete-branch"
  elif [[ $seg =~ ^[[:space:]]*gh[[:space:]]+api([[:space:]]|$) ]]; then
    grep -q 'git/refs' <<<"$seg" &&
      grep -qiE '(-X[[:space:]]*|--method[[:space:]=]+)DELETE' <<<"$seg" && block "gh api DELETE git/refs"
  fi
done <<<"$segments"
exit 0
