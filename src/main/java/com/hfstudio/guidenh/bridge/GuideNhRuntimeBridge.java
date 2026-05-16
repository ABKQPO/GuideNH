package com.hfstudio.guidenh.bridge;

import com.hfstudio.guidenh.GuideNH;

public class GuideNhRuntimeBridge {

    private GuideNhRuntimeBridgeServer server;

    public void start(GuideNhRuntimeBridgeSettings settings) {
        stop();
        if (!settings.canStart()) {
            GuideNH.LOG.info(
                "GuideNH runtime bridge start skipped. enabled={}, hostConfigured={}, portConfigured={}, tokenConfigured={}",
                settings.isEnabled(),
                !settings.getHost()
                    .isEmpty(),
                settings.getPort() > 0,
                !settings.getToken()
                    .isEmpty());
            return;
        }
        GuideNH.LOG.info(
            "Starting GuideNH runtime bridge. host={}, port={}, maxConnections={}, maxMessageBytes={}, maxPageSize={}, maxSubscriptions={}, maxDeltaEntries={}",
            settings.getHost(),
            settings.getPort(),
            settings.getMaxConnections(),
            settings.getMaxMessageBytes(),
            settings.getMaxPageSize(),
            settings.getMaxSubscriptions(),
            settings.getMaxDeltaEntries());
        server = new GuideNhRuntimeBridgeServer(settings);
        server.start();
    }

    public void stop() {
        if (server != null) {
            GuideNH.LOG.info("Stopping GuideNH runtime bridge");
            server.stop();
            server = null;
        }
    }

    public boolean isRunning() {
        return server != null && server.isRunning();
    }
}
