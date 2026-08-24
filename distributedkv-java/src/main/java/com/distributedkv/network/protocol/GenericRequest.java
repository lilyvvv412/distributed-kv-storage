package com.distributedkv.network.protocol;

/**
 * Concrete request used by server/client scaffolding and NodeManager.
 */
public class GenericRequest extends Request {

    private String raftGroupId;
    private String key;
    private byte[] value;

    public GenericRequest() {
        this(Type.STATUS, "local", "local", "default", 5000L);
    }

    public GenericRequest(Type type, String sourceNodeId, String destNodeId, String groupId, long timeoutMs) {
        super(type, sourceNodeId, destNodeId, groupId, timeoutMs);
    }

    public void setType(Type type) {
        // Type is final on Message; recreate via headers for routing hints.
        addHeader("effectiveType", type.name());
    }

    public String getRequestId() {
        return getId();
    }

    public String getRaftGroupId() {
        return raftGroupId != null ? raftGroupId : getGroupId();
    }

    public void setRaftGroupId(String raftGroupId) {
        this.raftGroupId = raftGroupId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }
}
