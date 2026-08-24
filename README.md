# DistributedKV

Distributed key-value storage platform combining a Java Multi-Raft storage
implementation with a Go sharded-KV implementation. The two implementations
are unified by the repository-level demo and evidence harness; they are not
mixed as peers in one cross-language Raft group.

## Architecture

```text
Unified Demo / Benchmark Harness
              |
      +-------+--------+
      |                |
Java Multi-Raft     Go Sharded-KV
cluster             cluster
      |                |
RocksDB/B+Tree/    Raft + Shard Controller
HashTable + MVCC   reconfiguration + migration
```

### Java implementation

`distributedkv-java/` contains the resume-critical storage path:

- Multi-Raft and linearizable KV operations;
- ReadIndex, FollowerRead, and asynchronous apply;
- consistent hashing and a local shard-migration path;
- RocksDB, B+ tree, and hash-table storage engines;
- MVCC version reads;
- Java/Netty/Protobuf networking and metrics.

### Go implementation

`go-extracted/raft-course-example/` contains the sharded-KV track:

- Raft replication and KV service;
- shard controller and ShardKV;
- join/leave reconfiguration and shard transfer;
- partial-outage and migration tests.

## Reproduce the unified demo

From this directory:

```bash
export PATH="$PWD/.tools/go/bin:$PATH"
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-24.jdk/Contents/Home
./run-distributedkv-demo.sh
```

The verified demo runs Java resume-critical smoke tests followed by Go
`TestJoinLeave` and `TestStaticShards`, then writes timestamped reports under
`reports/`.

Latest verified result:

- Java smoke suite: PASS;
- Go ShardKV reconfiguration/failure demo: PASS;
- unified result: PASS.

## Design → Evidence

| Design concept | Implementation / evidence |
|----------------|---------------------------|
| Multi-Raft | Java Raft group path + Go ShardKV Raft groups |
| ReadIndex | `ReadIndexTest` |
| FollowerRead | `FollowerReadSmokeTest` + `./run-follower-read-benchmark.sh` |
| Async Apply | `AsyncApplierSmokeTest` |
| Migration | Java `DataMigrationSmokeTest` + Go `TestJoinLeave` |
| Storage engines | `StorageEnginesSmokeTest` |
| MVCC | `MVCCSmokeTest` |
| Failure recovery | Go `TestStaticShards` + `./run-leader-failure-demo.sh` |

Full mapping: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md). Progress board: [`PROGRESS.md`](PROGRESS.md).

### Performance / failure evidence commands

```bash
./run-follower-read-benchmark.sh   # Leader vs FollowerRead latency report
./run-leader-failure-demo.sh       # kill leader → re-elect → R/W still works
```

Reports land in `reports/` (`follower-read-benchmark-latest.md`,
`leader-failure-demo-latest.md`, `distributedkv-unified-*.md`).

For a visual walkthrough, open the [Evidence Dashboard](evidence-dashboard/index.html)
or serve the repository with `python3 -m http.server 8080` and visit
`/evidence-dashboard/`.

## Scope of performance claims

Quote FollowerRead improvement from the measured report, not a fixed 42%.
Treat “3 Nines” as design/demo evidence (election + replication), not a
production SLO. Do not claim 1M QPS without a documented hardware/workload run.

## Known non-blocking issue

The full educational Go Raft suite has a known `TestCountPartB` RPC-budget
failure. The focused ShardKV reconfiguration and partial-outage tests used by
the unified demo pass. The extended ShardKV shard-deletion challenge currently
hits a snapshot-index bounds panic after the standard reconfiguration,
concurrency, restart, and unreliable-network cases pass. Legacy Java
API/integration suites remain excluded from the smoke command while the
resume-critical paths are being hardened.
