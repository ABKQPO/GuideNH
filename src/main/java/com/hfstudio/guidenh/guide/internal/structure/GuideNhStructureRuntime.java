package com.hfstudio.guidenh.guide.internal.structure;

public class GuideNhStructureRuntime {

    public static final GuideStructurePlacementService PLACEMENT_SERVICE = new GuideStructurePlacementService();
    public static final GuideStructureMemoryStore CLIENT_MEMORY_STORE = new GuideStructureMemoryStore(
        PLACEMENT_SERVICE);
    public static final GuideStructureServerSessionStore SERVER_SESSION_STORE = new GuideStructureServerSessionStore(
        PLACEMENT_SERVICE);

    public static volatile boolean serverStructureCommandsAvailable = false;
    public static volatile boolean clientStructureSyncNeeded = false;

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
