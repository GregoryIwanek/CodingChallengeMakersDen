#!/usr/bin/env bash
file=$(jq -r '.tool_input.file_path // empty')
[[ "$file" == *.kt || "$file" == *.kts ]] || exit 0
cd "$CLAUDE_PROJECT_DIR" && ./gradlew ktlintFormat -q --console=plain >/dev/null 2>&1 || true
