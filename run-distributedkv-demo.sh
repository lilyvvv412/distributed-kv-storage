#!/usr/bin/env bash
# Unified polyglot DistributedKV demo: Java smoke suite + Go ShardKV demo.
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
REPORT_DIR="$ROOT/reports"
mkdir -p "$REPORT_DIR"
TS="$(date +%Y%m%d-%H%M%S)"
UNIFIED="$REPORT_DIR/distributedkv-unified-$TS.md"

export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home}"
export PATH="$JAVA_HOME/bin:$PATH"
if [[ -x "$ROOT/.tools/go/bin/go" ]]; then
  export PATH="$ROOT/.tools/go/bin:$PATH"
fi

JAVA_STATUS=0
GO_STATUS=0

echo "========================================"
echo " DistributedKV Unified Demo"
echo "========================================"

echo
echo "==> [1/2] Java resume-critical smoke tests"
JAVA_LOG="$REPORT_DIR/java-smoke-$TS.log"
set +e
(
  cd "$ROOT/distributedkv-java"
  ./gradlew test \
    --tests 'com.distributedkv.storage.*' \
    --tests 'com.distributedkv.common.ConsistentHashSmokeTest' \
    --tests 'com.distributedkv.raft.*'
) >"$JAVA_LOG" 2>&1
JAVA_STATUS=$?
set -e
tail -n 40 "$JAVA_LOG" || true

echo
echo "==> [2/2] Go ShardKV reconfiguration + failure demo"
GO_LOG="$REPORT_DIR/go-demo-run-$TS.log"
set +e
(
  cd "$ROOT/go-extracted/raft-course-example"
  ./demo.sh
) >"$GO_LOG" 2>&1
GO_STATUS=$?
set -e
tail -n 60 "$GO_LOG" || true

{
  echo "# DistributedKV Unified Demo Report"
  echo
  echo "- Timestamp: $(date -u +%Y-%m-%dT%H:%M:%SZ)"
  echo "- Platform: polyglot (Java Multi-Raft storage + Go sharded KV)"
  echo
  echo "## Summary"
  echo
  if [[ $JAVA_STATUS -eq 0 ]]; then
    echo "- Java smoke suite: **PASS**"
  else
    echo "- Java smoke suite: **FAIL** (exit $JAVA_STATUS)"
  fi
  if [[ $GO_STATUS -eq 0 ]]; then
    echo "- Go ShardKV demo: **PASS**"
  else
    echo "- Go ShardKV demo: **FAIL** (exit $GO_STATUS)"
  fi
  echo
  echo "## Resume coverage exercised"
  echo
  echo "| Claim | Evidence |"
  echo "|-------|----------|"
  echo "| Multi-Raft / ReadIndex / FollowerRead / AsyncApply | Java raft smoke tests |"
  echo "| RocksDB / B+Tree / HashTable / MVCC | Java storage smoke tests |"
  echo "| Consistent hashing | Java ConsistentHashSmokeTest |"
  echo "| Local data migration path | Java DataMigrationSmokeTest |"
  echo "| Shard reconfiguration + migration between groups | Go \`TestJoinLeave\` |"
  echo "| Fault tolerance (partial outage) | Go \`TestStaticShards\` |"
  echo
  echo "## Artifacts"
  echo
  echo "- Java log: \`$JAVA_LOG\`"
  echo "- Go log: \`$GO_LOG\`"
  echo "- Latest Go markdown report under \`$REPORT_DIR/go-shardkv-demo-*.md\`"
  echo
  echo "## Notes"
  echo
  echo "- 1M QPS / 3 Nines / 42% latency are **design targets or measured under specific hardware**, not claimed by this demo alone."
  echo "- Java and Go clusters are unified at the demo layer; they do not form a single cross-language Raft group."
} > "$UNIFIED"

echo
echo "==> Unified report: $UNIFIED"
if [[ $JAVA_STATUS -eq 0 && $GO_STATUS -eq 0 ]]; then
  echo "==> OVERALL: PASS"
  exit 0
fi
echo "==> OVERALL: FAIL (java=$JAVA_STATUS go=$GO_STATUS)"
exit 1
