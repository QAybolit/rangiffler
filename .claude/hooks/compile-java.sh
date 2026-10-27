#!/usr/bin/env bash
set -uo pipefail

file=$(jq -r '.tool_response.filePath // .tool_input.file_path // empty')
case "$file" in
  *.java) ;;
  *) exit 0 ;;
esac

root=${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}
rel=${file#"$root"/}
module=${rel%%/*}
case "$module" in
  rfr-auth | rfr-api | rfr-e2e) ;;
  *) exit 0 ;;
esac

if [[ "$rel" == */src/test/* ]]; then
  task=compileTestJava
else
  task=compileJava
fi

cd "$root" || exit 0
output=$(./gradlew --console=plain -q ":$module:$task" 2>&1)
status=$?

if [ "$status" -eq 0 ]; then
  jq -n --arg message "Java compile OK (:$module:$task) after editing $rel" \
    '{hookSpecificOutput: {hookEventName: "PostToolUse", additionalContext: $message}}'
else
  jq -n --arg reason "Java compile FAILED (:$module:$task) after editing $rel:"$'\n'"$(printf '%s' "$output" | tail -60)" \
    '{decision: "block", reason: $reason}'
fi
