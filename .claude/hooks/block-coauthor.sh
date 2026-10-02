#!/usr/bin/env bash
cmd=$(jq -r '.tool_input.command // empty')
if [[ "$cmd" == *"git commit"* ]] && grep -qi 'co-authored-by' <<<"$cmd"; then
  echo "Blocked: Co-Authored-By trailers are banned in this repo (see .claude/CLAUDE.md). Commit again without the trailer." >&2
  exit 2
fi
exit 0
