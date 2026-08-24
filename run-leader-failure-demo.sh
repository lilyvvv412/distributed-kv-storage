#!/usr/bin/env bash
# Leader kill → re-election → continued R/W availability demo (Go kvraft).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
GO_ROOT="$ROOT/go-extracted/raft-course-example"
SRC="$GO_ROOT/src"
REPORT_DIR="$ROOT/reports"
mkdir -p "$REPORT_DIR"

if [[ -x "$ROOT/.tools/go/bin/go" ]]; then
  export PATH="$ROOT/.tools/go/bin:$PATH"
fi

if ! command -v go >/dev/null 2>&1; then
  echo "ERROR: go not found. Install Go or extract toolchain to .tools/go" >&2
  exit 1
fi

TS="$(date +%Y%m%d-%H%M%S)"
OUT="$REPORT_DIR/leader-failure-demo-$TS.md"
LOG="$REPORT_DIR/leader-failure-demo-$TS.log"
LATEST="$REPORT_DIR/leader-failure-demo-latest.md"

echo "==> Go version: $(go version)"
echo "==> Running TestLeaderFailureDemo..."

set +e
(
  cd "$SRC"
  go test ./kvraft -run '^TestLeaderFailureDemo$' -count=1 -timeout 5m -v
) >"$LOG" 2>&1
STATUS=$?
set -e
cat "$LOG"

{
  echo "# Leader Failure / Re-election Demo"
  echo
  echo "- Timestamp: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "- Go: \`$(go version)\`"
  echo "- Suite: \`kvraft.TestLeaderFailureDemo\`"
  echo "- Scenario: 3-node group → write → kill leader → wait election → write/read → verify"
  echo "- Exit code: $STATUS"
  echo
  if [[ $STATUS -eq 0 ]]; then
    echo "## Result: PASS"
    echo
    echo "- Automatic leader election observed after leader kill."
    echo "- Prior key remained readable; subsequent write/read succeeded on the new leader."
    echo "- Evidence for high-availability *design* (replication + re-election), not a production 99.9% SLO."
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
} | tee "$OUT" > "$LATEST"

echo "==> Wrote $OUT"
echo "==> Wrote $LATEST"
exit "$STATUS"
