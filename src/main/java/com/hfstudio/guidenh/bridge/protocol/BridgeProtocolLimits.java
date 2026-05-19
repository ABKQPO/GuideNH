package com.hfstudio.guidenh.bridge.protocol;

public class BridgeProtocolLimits {

    private final int maxMessageBytes;
    private final int maxPageSize;
    private final int maxSubscriptions;
    private final int maxConnections;
    private final int maxDeltaEntries;

    public BridgeProtocolLimits(int maxMessageBytes, int maxPageSize, int maxSubscriptions, int maxConnections,
        int maxDeltaEntries) {
        this.maxMessageBytes = maxMessageBytes;
        this.maxPageSize = maxPageSize;
        this.maxSubscriptions = maxSubscriptions;
        this.maxConnections = maxConnections;
        this.maxDeltaEntries = maxDeltaEntries;
    }

    public int getMaxMessageBytes() {
        return maxMessageBytes;
    }

    public int getMaxPageSize() {
        return maxPageSize;
    }

    public int getMaxSubscriptions() {
        return maxSubscriptions;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getMaxDeltaEntries() {
        return maxDeltaEntries;
    }
}
