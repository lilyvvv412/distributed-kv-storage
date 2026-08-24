package com.distributedkv.network.protocol;

/**
 * Leader response for a FollowerRead request.
 */
public class FollowerReadResponseMessage extends RaftMessage {

    private final String readId;
    private final boolean safeToRead;
    private final Long requiredAppliedIndex;
    private final String errorMessage;

    /** Immediate safe read (follower already caught up). */
    public FollowerReadResponseMessage(long term, String groupId, String nodeId, String readId) {
        super(term, groupId, nodeId);
        this.readId = readId;
        this.safeToRead = true;
        this.requiredAppliedIndex = null;
        this.errorMessage = null;
    }

    /** Safe to read after catching up to required index. */
    public FollowerReadResponseMessage(long term, String groupId, String nodeId,
                                       String readId, long requiredAppliedIndex) {
        super(term, groupId, nodeId);
        this.readId = readId;
        this.safeToRead = true;
        this.requiredAppliedIndex = requiredAppliedIndex;
        this.errorMessage = null;
    }

    /** Error / not safe. */
    public FollowerReadResponseMessage(long term, String groupId, String nodeId,
                                       String readId, String errorMessage) {
        super(term, groupId, nodeId);
        this.readId = readId;
        this.safeToRead = false;
        this.requiredAppliedIndex = null;
        this.errorMessage = errorMessage;
    }

    public String getReadId() {
        return readId;
    }

    public boolean isSafeToRead() {
        return safeToRead;
    }

    public boolean hasRequiredIndex() {
        return requiredAppliedIndex != null;
    }

    public long getRequiredAppliedIndex() {
        return requiredAppliedIndex != null ? requiredAppliedIndex : -1L;
    }

    public boolean hasError() {
        return errorMessage != null;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
