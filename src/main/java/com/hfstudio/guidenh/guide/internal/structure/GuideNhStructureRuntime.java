package com.hfstudio.guidenh.guide.internal.structure;

public final class GuideNhStructureRuntime {

    private static final GuideStructurePlacementService PLACEMENT_SERVICE = new GuideStructurePlacementService();
    private static final GuideStructureMemoryStore CLIENT_MEMORY_STORE = new GuideStructureMemoryStore(
        PLACEMENT_SERVICE);
    private static final GuideStructureServerSessionStore SERVER_SESSION_STORE = new GuideStructureServerSessionStore(
        PLACEMENT_SERVICE);

    private static volatile boolean serverStructureCommandsAvailable = false;
    private static volatile boolean clientStructureSyncNeeded = false;

    private GuideNhStructureRuntime() {}

    public static GuideStructurePlacementService getPlacementService() {
        return PLACEMENT_SERVICE;
    }

    public static GuideStructureMemoryStore getClientMemoryStore() {
        return CLIENT_MEMORY_STORE;
    }

    public static GuideStructureServerSessionStore getServerSessionStore() {
        return SERVER_SESSION_STORE;
    }

    public static boolean isServerStructureCommandsAvailable() {
        return serverStructureCommandsAvailable;
    }

    public static void setServerStructureCommandsAvailable(boolean available) {
        serverStructureCommandsAvailable = available;
    }

    public static boolean isClientStructureSyncNeeded() {
        return clientStructureSyncNeeded;
    }

    public static void setClientStructureSyncNeeded(boolean needed) {
        clientStructureSyncNeeded = needed;
    }
}
