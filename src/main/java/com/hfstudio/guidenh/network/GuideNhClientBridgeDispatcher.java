package com.hfstudio.guidenh.network;

final class GuideNhClientBridgeDispatcher {

    private GuideNhClientBridgeDispatcher() {}

    static void dispatch(GuideNhClientBridgeMessage message, ClientTaskScheduler scheduler,
        ImportStructureAction importStructureAction) {
        if (message.getAction() != GuideNhClientBridgeMessage.ACTION_IMPORT_STRUCTURE) {
            return;
        }
        scheduler
            .schedule(() -> importStructureAction.beginImportStructure(message.getX(), message.getY(), message.getZ()));
    }

    @FunctionalInterface
    interface ClientTaskScheduler {

        void schedule(Runnable task);
    }

    @FunctionalInterface
    interface ImportStructureAction {

        void beginImportStructure(int x, int y, int z);
    }
}
