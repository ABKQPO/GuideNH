package com.hfstudio.guidenh.guide.internal;

final class GuideMEClientReloadDispatcher {

    private GuideMEClientReloadDispatcher() {}

    static boolean dispatch(boolean onClientThread, ClientTaskScheduler scheduler, ReloadAction reloadAction) {
        if (reloadAction == null) {
            return false;
        }
        if (onClientThread) {
            reloadAction.reload();
        } else if (scheduler != null) {
            scheduler.schedule(reloadAction::reload);
        } else {
            return false;
        }
        return true;
    }

    @FunctionalInterface
    interface ClientTaskScheduler {

        void schedule(Runnable task);
    }

    @FunctionalInterface
    interface ReloadAction {

        void reload();
    }
}
