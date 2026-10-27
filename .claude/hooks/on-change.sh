#!/usr/bin/env bash
set -uo pipefail

cat > /dev/null
root=${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}
cd "$root" || exit 0
state="$(git rev-parse --git-dir)/claude-snapshot"
current=$({ git diff HEAD --name-only; git ls-files -o --exclude-standard; } | sort -u | while read -r f; do [ -f "$f" ] && shasum "$f"; done)
previous=$(cat "$state" 2>/dev/null || true)
printf '%s\n' "$current" > "$state"
changed=$(comm -13 <(printf '%s\n' "$previous" | sort) <(printf '%s\n' "$current" | sort) | awk '{print $2}')
[ -z "$changed" ] && exit 0

for file in $changed; do
  for check in compile-java.sh compile-frontend.sh; do
    out=$(printf '{"tool_input":{"file_path":"%s/%s"}}' "$root" "$file" | ".claude/hooks/$check")
    case "$out" in
      *'"decision": "block"'*)
        printf '%s\n' "$out"
        exit 0
        ;;
    esac
  done
done
