# Go Sharded-KV Track

This directory is the **Go sharded key-value** track of the polyglot
[DistributedKV](../../PROGRESS.md) platform.

It is based on the MIT 6.5840 / raft-course ShardKV labs and provides:

- Raft replication
- KV server (`kvraft`)
- Shard controller (`shardctrler`)
- Sharded KV with reconfiguration and shard migration (`shardkv`)
- Fault-injection style tests (unreliable network, join/leave, group shutdown)

Java Multi-Raft / storage engines live in `../../distributedkv-java`.
Do **not** expect Java and Go peers to form one Raft group; they are unified at
the demo/report layer.

## Prerequisites

- Go 1.20+ (project ships a local toolchain under `../../.tools/go` if needed)

```bash
export PATH="$(pwd)/../../.tools/go/bin:$PATH"   # if using project-local Go
go version
```

## Layout

```
src/
  raft/          Raft consensus
  kvraft/        Replicated KV
  shardctrler/   Shard configuration controller
  shardkv/       Sharded KV + migration between groups
  labrpc/        Test RPC network with fault injection
```

## Run tests

```bash
cd src
go test ./labgob ./labrpc -count=1
go test ./raft -count=1 -timeout 15m   # note: TestCountPartB may fail on RPC-count budget
go test ./shardctrler -count=1
go test ./shardkv -count=1 -timeout 30m
```

Known issue: `raft.TestCountPartB` fails on this tree (election/idle RPC count over budget).
Functional Raft parts A/C/D and ShardKV migration tests still pass; demo uses ShardKV suites.

### Resume-critical availability / migration tests

| Test | What it shows |
|------|----------------|
| `kvraft.TestLeaderFailureDemo` | Kill leader → re-elect → same key still R/W |
| `TestJoinLeave` | Reconfiguration + shard migration across groups |
| `TestStaticShards` | Partial unavailability with one shard group down |
| `TestSnapshot` | Snapshots with join/leave |
| `TestUnreliable*` | Faulty network tolerance |

```bash
# From repo root
../../run-leader-failure-demo.sh
# or:
cd src && go test ./kvraft -run '^TestLeaderFailureDemo$' -v
```

```bash
cd src
go test ./shardkv -run 'TestJoinLeave|TestStaticShards' -count=1 -timeout 10m -v
```

## Demo command

From this directory:

```bash
./demo.sh
```

Or from the repo root unified harness:

```bash
../../run-distributedkv-demo.sh
```

`demo.sh` runs a focused ShardKV reconfiguration + failure suite and writes a
markdown snippet under `../../reports/`.
