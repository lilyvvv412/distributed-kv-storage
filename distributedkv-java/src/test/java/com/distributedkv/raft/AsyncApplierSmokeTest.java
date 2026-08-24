package com.distributedkv.raft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Smoke test that AsyncApplier can apply an entry without blocking the caller path.
 */
public class AsyncApplierSmokeTest {

    @Test
    void applyImmediateUpdatesLastApplied() throws Exception {
        StateMachine stateMachine = mock(StateMachine.class);
        Log log = mock(Log.class);

        when(stateMachine.apply(any(LogEntry.class))).thenReturn("ok".getBytes());
        when(log.getCommitIndex()).thenReturn(1L);

        AsyncApplier applier = new AsyncApplier(stateMachine, log, 16);
        LogEntry entry = new LogEntry(1L, 1L, "cmd".getBytes(), LogEntry.EntryType.NORMAL);

        byte[] result = applier.applyImmediate(entry);
        assertEquals("ok", new String(result));
        assertEquals(1L, applier.getLastAppliedIndex());

        applier.waitForApplied(1L).get(1, TimeUnit.SECONDS);
        applier.stop();
    }
}
