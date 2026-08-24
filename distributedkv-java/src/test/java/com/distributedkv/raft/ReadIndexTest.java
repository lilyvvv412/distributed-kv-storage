package com.distributedkv.raft;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.distributedkv.network.protocol.ReadIndexRequestMessage;
import com.distributedkv.network.protocol.ReadIndexResponseMessage;

/**
 * Focused ReadIndex unit test against the current API surface.
 */
public class ReadIndexTest {

    private RaftNode raftNode;
    private AsyncApplier asyncApplier;
    private ReadIndex readIndex;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        raftNode = mock(RaftNode.class);
        asyncApplier = mock(AsyncApplier.class);

        when(raftNode.isLeader()).thenReturn(true);
        when(raftNode.getCurrentTerm()).thenReturn(1L);
        when(raftNode.getGroupId()).thenReturn("g1");
        when(raftNode.getNodeId()).thenReturn("n1");
        when(raftNode.getCommitIndex()).thenReturn(10L);
        when(raftNode.broadcastHeartbeats()).thenReturn(CompletableFuture.completedFuture(true));
        when(asyncApplier.getLastAppliedIndex()).thenReturn(10L);
        when(asyncApplier.waitForApplied(anyLong())).thenReturn(CompletableFuture.completedFuture(null));

        Function<ReadIndexRequestMessage, CompletableFuture<ReadIndexResponseMessage>> forwarder =
                req -> CompletableFuture.completedFuture(
                        new ReadIndexResponseMessage(1L, "g1", "n1", req.getReadId(), 10L));

        readIndex = new ReadIndex(raftNode, asyncApplier, forwarder, 1000);
    }

    @Test
    void leaderReadIndexReturnsCommitIndex() throws Exception {
        Long index = readIndex.processRead("key".getBytes()).get(2, TimeUnit.SECONDS);
        assertNotNull(index);
        assertTrue(index >= 0);
    }

    @Test
    void leaderCanServeReadOperation() throws Exception {
        byte[] value = "v".getBytes();
        when(raftNode.isLeader()).thenReturn(true);

        String result = readIndex.read("k".getBytes(), key -> new String(value))
                .get(2, TimeUnit.SECONDS);
        assertNotNull(result);
    }
}
