package kvraft

import (
	"testing"
	"time"
)

// TestLeaderFailureDemo exercises a resume-facing availability path:
//   start 3-node group → write key → kill leader → wait for election →
//   write/read same key → verify data remains available.
//
// This demonstrates automatic leader election + replication under failure.
// It is design/demo evidence for high availability, not a measured 99.9% SLO.
func TestLeaderFailureDemo(t *testing.T) {
	const nservers = 3
	cfg := make_config(t, nservers, false, -1)
	defer cfg.cleanup()

	ck := cfg.makeClient(cfg.All())
	cfg.begin("Demo: kill leader, re-elect, continue R/W")

	const key = "demo-availability-key"
	Put(cfg, ck, key, "before-kill", nil, -1)
	check(cfg, t, ck, key, "before-kill")

	ok, leader := cfg.Leader()
	if !ok {
		t.Fatalf("expected a leader before failure injection")
	}
	t.Logf("killing leader server %d", leader)
	cfg.ShutdownServer(leader)

	// Allow an election (tester budget is generous vs paper timeouts).
	deadline := time.Now().Add(3 * electionTimeout)
	newLeader := -1
	for time.Now().Before(deadline) {
		found, id := cfg.Leader()
		if found && id != leader {
			newLeader = id
			break
		}
		time.Sleep(50 * time.Millisecond)
	}
	if newLeader < 0 {
		t.Fatalf("no new leader elected after killing server %d", leader)
	}
	t.Logf("new leader elected: server %d", newLeader)

	// Surviving majority should keep serving; prior value must still be readable.
	check(cfg, t, ck, key, "before-kill")
	Put(cfg, ck, key, "after-election", nil, -1)
	check(cfg, t, ck, key, "after-election")

	cfg.end()
}
