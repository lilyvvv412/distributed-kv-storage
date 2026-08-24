package com.distributedkv.network.protocol;

/**
 * Follower-to-leader request used by the FollowerRead optimization.
 */
public class FollowerReadRequestMessage extends RaftMessage {

    private final String readId;
    private final byte[] key;
    private final long readTimestamp;
    private final long lastAppliedIndex;

    public FollowerReadRequestMessage(long term, String groupId, String nodeId,
                                      String readId, byte[] key,
                                      long readTimestamp, long lastAppliedIndex) {
        super(term, groupId, nodeId);
        this.readId = readId;
        this.key = key;
        this.readTimestamp = readTimestamp;
        this.lastAppliedIndex = lastAppliedIndex;
    }

    public String getReadId() {
        return readId;
    }

    public byte[] getKey() {
        return key;
    }

    public long getReadTimestamp() {
        return readTimestamp;
    }

    public long getLastAppliedIndex() {
        return lastAppliedIndex;
    }
}
