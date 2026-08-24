#!/usr/bin/env bash
# Focused Go ShardKV demo: reconfiguration/migration + failure evidence.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT/src"
REPORT_DIR="$(cd "$ROOT/../.." && pwd)/reports"
mkdir -p "$REPORT_DIR"

# Prefer project-local Go if present.
if [[ -x "$(cd "$ROOT/../.." && pwd)/.tools/go/bin/go" ]]; then
  export PATH="$(cd "$ROOT/../.." && pwd)/.tools/go/bin:$PATH"
fi

if ! command -v go >/dev/null 2>&1; then
  echo "ERROR: go not found. Install Go or extract toolchain to .tools/go" >&2
  exit 1
fi

TS="$(date +%Y%m%d-%H%M%S)"
OUT="$REPORT_DIR/go-shardkv-demo-$TS.md"
LOG="$REPORT_DIR/go-shardkv-demo-$TS.log"

echo "==> Go version: $(go version)"
echo "==> Running ShardKV demo tests (JoinLeave + StaticShards)..."

set +e
(
  cd "$SRC"
  go test ./shardkv -run 'TestJoinLeave|TestStaticShards' -count=1 -timeout 15m -v
) >"$LOG" 2>&1
STATUS=$?
set -e
cat "$LOG"

{
  echo "# Go ShardKV Demo Report"
  echo
  echo "- Timestamp: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "- Go: \`$(go version)\`"
  echo "- Suite: \`TestJoinLeave|TestStaticShards\`"
  echo "- Exit code: $STATUS"
  echo
  if [[ $STATUS -eq 0 ]]; then
    echo "## Result: PASS"
    echo
    echo "- **Reconfiguration / migration:** \`TestJoinLeave\` exercised join → leave → shard transfer → source group shutdown while data remained readable."
    echo "- **Fault tolerance:** \`TestStaticShards\` exercised partial group unavailability while other shards continued to serve."
  else
    echo "## Result: FAIL"
    echo
    echo "See log: \`$LOG\`"
  fi
  echo
  echo "## Log excerpt"
  echo
  echo '```'
  tail -n 80 "$LOG"
  echo '```'
} > "$OUT"

echo "==> Wrote $OUT"
exit "$STATUS"
