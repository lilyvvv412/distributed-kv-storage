#!/usr/bin/env bash
# FollowerRead vs Leader ReadIndex latency benchmark (path-level, modeled RTTs).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
REPORT_DIR="$ROOT/reports"
mkdir -p "$REPORT_DIR"

export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"

echo "==> FollowerRead latency benchmark"
echo "==> JAVA_HOME=$JAVA_HOME"
echo "==> Reports → $REPORT_DIR"

cd "$ROOT/distributedkv-java"
./gradlew -q test --tests 'com.distributedkv.raft.FollowerReadLatencyBenchmark' \
  -Ddistributedkv.reportDir="$REPORT_DIR"

LATEST="$REPORT_DIR/follower-read-benchmark-latest.md"
if [[ -f "$LATEST" ]]; then
  echo
  echo "==> Latest report: $LATEST"
  cat "$LATEST"
else
  echo "WARN: expected $LATEST was not written" >&2
  exit 1
fi
