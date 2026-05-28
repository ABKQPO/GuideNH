package com.hfstudio.guidenh.bridge;

public class GuideNhRuntimeBridgeSettings {

    private final boolean enabled;
    private final String host;
    private final int port;
    private final String token;
    private final int maxMessageBytes;
    private final int maxPageSize;
    private final int maxSubscriptions;
    private final int maxConnections;
    private final int maxDeltaEntries;

    public GuideNhRuntimeBridgeSettings(boolean enabled, String host, int port, String token, int maxMessageBytes,
        int maxPageSize, int maxSubscriptions, int maxConnections, int maxDeltaEntries) {
        this.enabled = enabled;
        this.host = host == null ? "" : host.trim();
        this.port = port;
        this.token = token == null ? "" : token;
        this.maxMessageBytes = maxMessageBytes;
        this.maxPageSize = maxPageSize;
        this.maxSubscriptions = maxSubscriptions;
        this.maxConnections = maxConnections;
        this.maxDeltaEntries = maxDeltaEntries;
    }

    public boolean canStart() {
        return enabled && !host.isEmpty() && port > 0 && port <= 65535 && !token.isEmpty();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getToken() {
        return token;
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
