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
lint_output=$(npx --no-install eslint --report-unused-disable-directives --max-warnings 0 "${rel#rfr-gql-client/}" 2>&1)
lint_status=$?

if [ "$status" -eq 0 ] && [ "$lint_status" -eq 0 ]; then
  jq -n --arg message "Frontend type check and ESLint OK (tsc --noEmit, eslint) after editing $rel" \
    '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $message}}'
else
  failures=""
  [ "$status" -ne 0 ] && failures+="Frontend type check FAILED (tsc --noEmit) after editing $rel:"$'\n'"$(printf '%s' "$output" | tail -60)"$'\n'
  [ "$lint_status" -ne 0 ] && failures+="Frontend ESLint FAILED after editing $rel:"$'\n'"$(printf '%s' "$lint_output" | tail -60)"
  jq -n --arg reason "$failures" '{decision: "block", reason: $reason}'
fi
