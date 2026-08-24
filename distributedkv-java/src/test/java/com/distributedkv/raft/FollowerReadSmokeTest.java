package com.distributedkv.raft;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.distributedkv.network.protocol.FollowerReadRequestMessage;
import com.distributedkv.network.protocol.FollowerReadResponseMessage;

/**
 * Smoke test for FollowerRead request processing on a leader.
 */
public class FollowerReadSmokeTest {

    @Test
    void leaderMarksCaughtUpFollowerSafeToRead() {
        RaftNode raftNode = mock(RaftNode.class);
        AsyncApplier asyncApplier = mock(AsyncApplier.class);
        ReadIndex readIndex = mock(ReadIndex.class);

        when(raftNode.isLeader()).thenReturn(true);
        when(raftNode.getCurrentTerm()).thenReturn(3L);
        when(raftNode.getGroupId()).thenReturn("g1");
        when(raftNode.getNodeId()).thenReturn("leader");
        when(raftNode.getCommitIndex()).thenReturn(20L);
        when(asyncApplier.getLastAppliedIndex()).thenReturn(20L);

        Function<FollowerReadRequestMessage, CompletableFuture<FollowerReadResponseMessage>> forwarder =
                req -> CompletableFuture.completedFuture(
                        new FollowerReadResponseMessage(3L, "g1", "leader", req.getReadId()));

        FollowerRead followerRead = new FollowerRead(
                raftNode, asyncApplier, readIndex, forwarder, 1000, 5000);

        FollowerReadRequestMessage request = new FollowerReadRequestMessage(
                3L, "g1", "follower", "r1", "k".getBytes(), System.currentTimeMillis(), 20L);

        FollowerReadResponseMessage response = followerRead.processFollowerReadRequest(request);
        assertNotNull(response);
        assertFalse(response.hasError());
    }
}
