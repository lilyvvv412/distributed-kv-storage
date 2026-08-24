# DistributedKV Architecture Evidence

This document maps design concepts to runnable implementation evidence.
The Java and Go tracks are unified by the repository demo/benchmark harness;
they are **not** peers in one cross-language Raft group.

## Design → Evidence

| Design concept | Implementation / evidence |
|----------------|---------------------------|
| Multi-Raft | Java `RaftGroup` / group management path + Go Raft groups in ShardKV |
| ReadIndex | `distributedkv-java` `ReadIndexTest` |
| FollowerRead | `FollowerReadSmokeTest` + `FollowerReadLatencyBenchmark` (`./run-follower-read-benchmark.sh`) |
| Async Apply | `AsyncApplierSmokeTest` |
| Migration | Java `DataMigrationSmokeTest` + Go `TestJoinLeave` |
| Storage engines | `StorageEnginesSmokeTest` (RocksDB / B+Tree / HashTable) |
| MVCC | `MVCCSmokeTest` |
| Failure recovery | Go `TestStaticShards` + `TestLeaderFailureDemo` (`./run-leader-failure-demo.sh`) |
| Unified platform story | Root `./run-distributedkv-demo.sh` → `reports/distributedkv-unified-*.md` |

## Reproduce

```bash
# From repo root
export PATH="$PWD/.tools/go/bin:$PATH"
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home

./run-distributedkv-demo.sh
./run-follower-read-benchmark.sh
./run-leader-failure-demo.sh
```

## Claim hygiene

| Topic | How to talk about it |
|-------|----------------------|
| FollowerRead latency | Quote the measured improvement from `reports/follower-read-benchmark-latest.md` |
| Availability / “3 Nines” | Describe election + replication demo evidence; do **not** claim a production SLO |
| 1M QPS | Capacity design target only until a documented hardware/workload benchmark exists |
| `TestCountPartB` | Known non-blocking educational Raft RPC-budget failure; not used by the unified demo |
