#!/usr/bin/env bash
set -uo pipefail

file=$(jq -r '.tool_response.filePath // .tool_input.file_path // empty')
case "$file" in
  *.ts | *.tsx) ;;
  *) exit 0 ;;
esac

root=${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}
rel=${file#"$root"/}
case "$rel" in
  rfr-gql-client/*) ;;
  *) exit 0 ;;
esac

cd "$root/rfr-gql-client" || exit 0
output=$(npx --no-install tsc --noEmit 2>&1)
status=$?

if [ "$status" -eq 0 ]; then
  jq -n --arg message "Frontend type check OK (tsc --noEmit) after editing $rel" \
    '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $message}}'
else
  jq -n --arg reason "Frontend type check FAILED (tsc --noEmit) after editing $rel:"$'\n'"$(printf '%s' "$output" | tail -60)" \
    '{decision: "block", reason: $reason}'
fi
