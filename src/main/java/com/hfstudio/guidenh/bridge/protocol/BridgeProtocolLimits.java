package com.hfstudio.guidenh.bridge.protocol;

public class BridgeProtocolLimits {

    private final int maxMessageBytes;
    private final int maxPageSize;
    private final int maxSubscriptions;
    private final int maxConnections;
    private final int maxDeltaEntries;
    private final int maxPreviewSearchPageSize;
    private final int maxPreviewResolveBytes;
    private final int maxPreviewIconPixels;
    private final int maxPreviewTooltipLines;

    public BridgeProtocolLimits(int maxMessageBytes, int maxPageSize, int maxSubscriptions, int maxConnections,
        int maxDeltaEntries) {
        this.maxMessageBytes = maxMessageBytes;
        this.maxPageSize = maxPageSize;
        this.maxSubscriptions = maxSubscriptions;
        this.maxConnections = maxConnections;
        this.maxDeltaEntries = maxDeltaEntries;
        this.maxPreviewSearchPageSize = Math.clamp(maxPageSize, 1, 80);
        this.maxPreviewResolveBytes = Math.clamp(maxMessageBytes - 4096, 32768, 131072);
        this.maxPreviewIconPixels = 128 * 128;
        this.maxPreviewTooltipLines = 24;
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

    public int getMaxPreviewSearchPageSize() {
        return maxPreviewSearchPageSize;
    }

    public int getMaxPreviewResolveBytes() {
        return maxPreviewResolveBytes;
    }

    public int getMaxPreviewIconPixels() {
        return maxPreviewIconPixels;
    }

    public int getMaxPreviewTooltipLines() {
        return maxPreviewTooltipLines;
    }
}
