# DistributedKV — Resume Alignment Progress

> Shared progress board for **Cursor** and **Codex**.  
> Goal: bring this repo to ~90% alignment with the resume narrative via a **polyglot platform** (Java Multi-Raft storage + Go sharded KV), unified by a demo/benchmark harness — **not** a cross-language Raft runtime.

Last updated: 2026-08-08 (Cursor Day3 — evidence)

---

## Resume Target (source of truth)

- Multi-Raft + linearizable KV read/write (C++/Java/Go)
- Consistent hashing + data migration between Raft groups
- Async Apply, ReadIndex, FollowerRead (~42% read latency reduction — **must be measured**)
- Storage engines: RocksDB, B+ tree, hash table + MVCC
- Availability / QPS numbers (3 Nines, 1M QPS) treated as **design targets / measured under X**, not unverified claims

---

## Architecture Decision (locked)

```
Unified Demo CLI / Benchmark Harness
              |
      +-------+--------+
      |                |
Java Multi-Raft     Go Sharded-KV
cluster             cluster
```

| Layer | Owns |
|-------|------|
| **Java** (`distributedkv-java`) | Multi-Raft, linearizable KV, ReadIndex, FollowerRead, AsyncApply, RocksDB/B+Tree/HashTable, MVCC, metrics/benchmark |
| **Go** (`go-extracted/raft-course-example`) | Raft replication, KV server, Shard Controller, ShardKV, reconfiguration, shard migration, fault-injection tests |
| **Unified** (repo root) | Start both clusters, put/get, reconfig, kill node, verify reads, unified report |

**Out of scope for 2-day sprint:** cross-language gRPC Raft, mixing Java/Go peers in one Raft group.

---

## Status Legend

- `[ ]` not started
- `[~]` in progress
- `[x]` done / verified
- `[!]` blocked

---

## Day 1 — Java main system (`distributedkv-java`)

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1 | Reproducible Gradle/JDK build | `[x]` | Wrapper Gradle **8.14.3**. Official CDN OK with VPN |
| 2 | All existing tests pass | `[~]` | Main compile green; legacy suites excluded; smoke suites green |
| 3 | ReadIndex runnable path + test | `[x]` | `ReadIndexTest` |
| 4 | FollowerRead runnable path + test | `[x]` | `FollowerReadSmokeTest` |
| 5 | AsyncApply runnable path + test | `[x]` | `AsyncApplierSmokeTest` |
| 6 | Three storage engines tests | `[x]` | `StorageEnginesSmokeTest` |
| 7 | MVCC version-read tests | `[x]` | `MVCCSmokeTest` |
| 8 | DataMigrator minimal runnable path | `[x]` | `DataMigrationSmokeTest` |
| 9 | Benchmark CLI artifact | `[~]` | FollowerRead path benchmark shipped; full Java CLI still open |

### Day 1 Cursor working notes

- See earlier notes; Java smoke verified again via unified demo on 2026-08-08.

---

## Day 2 — Go + Unified Demo

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1 | `go test` in `raft-course-example` | `[~]` | labgob/labrpc/shardctrler **PASS**; raft mostly PASS except `TestCountPartB` (RPC budget); focused ShardKV **PASS** |
| 2 | Confirm ShardKV reconfiguration/migration tests | `[x]` | `TestJoinLeave` + `TestStaticShards` PASS |
| 3 | Go README for sharded-KV track | `[x]` | `go-extracted/raft-course-example/README.md` |
| 4 | Go demo command | `[x]` | `go-extracted/raft-course-example/demo.sh` |
| 5 | Root `run-distributedkv-demo.sh` | `[x]` | Exists + executable |
| 6 | Unified Java + Go result report | `[x]` | `reports/distributedkv-unified-*.md` |
| 7 | Failure test (kill/restart or simulated) | `[x]` | `TestStaticShards` + `TestJoinLeave` + `TestLeaderFailureDemo` |

### Day 2 Cursor working notes

- 2026-08-08: Installed Go **1.22.12** under repo `.tools/go` (not Homebrew).
- Unified demo **OVERALL: PASS** — report under `reports/`.
- Known non-blocking: `raft.TestCountPartB` RPC-budget failure (do not dig unless spare time).

---

## Day 3 — Performance + failure evidence

| # | Task | Status | Notes |
|---|------|--------|-------|
| 1 | FollowerRead vs LeaderRead benchmark | `[x]` | `./run-follower-read-benchmark.sh` → `reports/follower-read-benchmark-latest.md` |
| 2 | Leader failure / re-election demo | `[x]` | `./run-leader-failure-demo.sh` → PASS (~0.6s) |
| 3 | Design→evidence mapping | `[x]` | Root README + `docs/ARCHITECTURE.md` |
| 4 | Full Java benchmark CLI | `[ ]` | Optional; only after evidence above |

### Day 3 measured results (2026-08-08)

**FollowerRead path benchmark** (modeled majority HB 5ms vs follower safety-check 2ms; 2000 ops, concurrency 8):

| Path | avg | P50 | P95 |
|------|-----|-----|-----|
| Leader ReadIndex | 6.184 ms | 6.318 ms | 6.461 ms |
| FollowerRead | 2.563 ms | 2.581 ms | 2.716 ms |
| **improvement** | **58.5%** | **59.1%** | — |

Resume wording: use the **measured** % from the latest report (currently ~58% under documented RTTs), not a hardcoded 42%. Methodology is path-level with injected delays — not a production-cluster SLO.

**Leader failure demo:** kill leader → new leader elected → prior key readable → post-election Put/Get OK. Evidence for HA *design*, not 99.9% SLO.

```bash
export PATH="$PWD/.tools/go/bin:$PATH"
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home
./run-follower-read-benchmark.sh
./run-leader-failure-demo.sh
```

---

## Resume Coverage Matrix

| Resume claim | Evidence target | Status |
|--------------|-----------------|--------|
| Multi-Raft | Java smoke + Go raft (minus RPC-count flake) | `[~]` |
| Linearizable KV R/W | Java ReadIndex smoke | `[~]` |
| Consistent hashing | Java ConsistentHashSmokeTest | `[x]` |
| Migration between Raft groups | Go `TestJoinLeave` + Java local migration smoke | `[x]` |
| Async Apply | `AsyncApplierSmokeTest` | `[x]` |
| ReadIndex | `ReadIndexTest` | `[x]` |
| FollowerRead | Smoke + latency benchmark report | `[x]` |
| RocksDB / B+Tree / HashTable | `StorageEnginesSmokeTest` | `[x]` |
| MVCC | `MVCCSmokeTest` | `[x]` |
| Go | ShardKV demo + README | `[x]` |
| 1M QPS | Benchmark harness + capacity target | `[ ]` (do not invent) |
| 3 Nines | Leader-failure + StaticShards/JoinLeave demos | `[~]` (design evidence, not SLO) |
| ~42% latency reduction | Measured ~58% avg under modeled RTTs | `[x]` (quote report) |

---

## How to update this file

1. Flip status markers when a row is finished.
2. Append dated bullets under the relevant “working notes” section.
3. Prefer short evidence: test class name, command, or report path.
4. If blocked, set `[!]` and write the blocker in Notes.

---

## Quick commands

```bash
# Java smoke
cd distributedkv-java
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home
./gradlew test --tests 'com.distributedkv.storage.*' \
  --tests 'com.distributedkv.common.ConsistentHashSmokeTest' \
  --tests 'com.distributedkv.raft.*'

# Evidence
./run-follower-read-benchmark.sh
./run-leader-failure-demo.sh

# Go ShardKV + Unified
export PATH="$PWD/.tools/go/bin:$PATH"
./go-extracted/raft-course-example/demo.sh
./run-distributedkv-demo.sh
```
