package com.hfstudio.guidenh.bridge;

public class GuideNhRuntimeBridge {

    private GuideNhRuntimeBridgeServer server;

    public void start(GuideNhRuntimeBridgeSettings settings) {
        stop();
        if (!settings.canStart()) {
            return;
        }
        server = new GuideNhRuntimeBridgeServer(settings);
        server.start();
    }

    public void stop() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    public boolean isRunning() {
        return server != null && server.isRunning();
    }
}
