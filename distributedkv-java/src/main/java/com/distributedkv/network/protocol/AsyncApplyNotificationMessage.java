package com.distributedkv.network.protocol;

/**
 * Notification emitted after an async apply completes.
 */
public class AsyncApplyNotificationMessage extends RaftMessage {

    private final long appliedIndex;
    private final long commitIndex;
    private final long applyTimeMs;
    private final String errorMessage;

    public AsyncApplyNotificationMessage(long term, String groupId, String nodeId,
                                         long appliedIndex, long commitIndex, long applyTimeMs) {
        this(term, groupId, nodeId, appliedIndex, commitIndex, applyTimeMs, null);
    }

    public AsyncApplyNotificationMessage(long term, String groupId, String nodeId,
                                         long appliedIndex, long commitIndex, long applyTimeMs,
                                         String errorMessage) {
        super(term, groupId, nodeId);
        this.appliedIndex = appliedIndex;
        this.commitIndex = commitIndex;
        this.applyTimeMs = applyTimeMs;
        this.errorMessage = errorMessage;
    }

    public long getAppliedIndex() {
        return appliedIndex;
    }

    public long getCommitIndex() {
        return commitIndex;
    }

    public long getApplyTimeMs() {
        return applyTimeMs;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public boolean isSuccess() {
        return !hasError();
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
