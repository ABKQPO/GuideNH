package com.hfstudio.guidenh.guide.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.config.ModConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import io.methvin.watcher.DirectoryChangeEvent;
import io.methvin.watcher.DirectoryChangeListener;
import io.methvin.watcher.DirectoryWatcher;

public class GuideDevelopmentResourcePackWatcher implements AutoCloseable {

    private final List<DirectoryWatcher> watchers = new ArrayList<>();
    private final ExecutorService watchExecutor;
    private final Runnable reloadAction;
    private final AtomicBoolean reloadRequested = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    private GuideDevelopmentResourcePackWatcher(Runnable reloadAction) {
        this.reloadAction = reloadAction;
        this.watchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "GuideNH-DevResourcePackWatcher");
            thread.setDaemon(true);
            return thread;
        });
    }

    public static void init() {
        if (!GuideDevelopmentResourcePacks.hasConfiguredPacks()) {
            return;
        }

        var watcher = new GuideDevelopmentResourcePackWatcher(GuideLightweightReloadService::reloadDevelopmentGuides);
        watcher.start(GuideDevelopmentResourcePacks.getConfiguredPacks());
        if (!watcher.isWatching()) {
            watcher.close();
            return;
        }
        Runtime.getRuntime()
            .addShutdownHook(new Thread(watcher::close, "GuideNH-DevResourcePackWatcherShutdown"));
        FMLCommonHandler.instance()
            .bus()
            .register(watcher);
    }

    public static GuideDevelopmentResourcePackWatcher createForTests(List<GuideDevelopmentResourcePack> packs) {
        return createForTests(packs, () -> {});
    }

    public static GuideDevelopmentResourcePackWatcher createForTests(List<GuideDevelopmentResourcePack> packs,
        Runnable reloadAction) {
        var watcher = new GuideDevelopmentResourcePackWatcher(reloadAction);
        watcher.start(packs);
        return watcher;
    }

    private void start(List<GuideDevelopmentResourcePack> packs) {
        for (GuideDevelopmentResourcePack pack : packs) {
            Path root = pack.getRoot();
            if (!Files.isDirectory(root)) {
                continue;
            }

            try {
                DirectoryWatcher watcher = DirectoryWatcher.builder()
                    .path(root)
                    .fileHashing(false)
                    .listener(new Listener())
                    .build();
                watcher.watchAsync(watchExecutor);
                watchers.add(watcher);
                logInfo("Watching development resource pack {}", root);
            } catch (IOException e) {
                logError("Failed to watch {}", root, e);
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (closed.get()) {
            return;
        }
        if (!reloadRequested.getAndSet(false)) {
            return;
        }

        reloadAction.run();
    }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        for (DirectoryWatcher watcher : watchers) {
            try {
                watcher.close();
            } catch (IOException ignored) {}
        }
        watchers.clear();
        watchExecutor.shutdownNow();
    }

    public boolean isWatching() {
        if (closed.get()) {
            return false;
        }
        for (DirectoryWatcher watcher : watchers) {
            if (!watcher.isClosed()) {
                return true;
            }
        }
        return false;
    }

    private void requestReload() {
        if (!closed.get()) {
            reloadRequested.set(true);
        }
    }

    public void requestReloadForTests() {
        requestReload();
    }

    public void processPendingReloadForTests() {
        if (!closed.get() && reloadRequested.getAndSet(false)) {
            reloadAction.run();
        }
    }

    private static void logInfo(String message, Object... args) {
        Logger logger = FMLLog.getLogger();
        if (logger != null && ModConfig.debug.enableDebugMode) {
            logger.info("[GuideNH] [GuideDevelopmentResourcePackWatcher] " + message, args);
        }
    }

    private static void logError(String message, Object arg, Throwable throwable) {
        Logger logger = FMLLog.getLogger();
        if (logger != null) {
            logger.error("[GuideNH] [GuideDevelopmentResourcePackWatcher] " + message, arg, throwable);
        }
    }

    private final class Listener implements DirectoryChangeListener {

        @Override
        public void onEvent(DirectoryChangeEvent event) {
            if (!event.isDirectory()) {
                requestReload();
            }
        }

        @Override
        public boolean isWatching() {
            return GuideDevelopmentResourcePackWatcher.this.isWatching();
        }

        @Override
        public void onException(Exception e) {
            Logger logger = FMLLog.getLogger();
            if (logger != null) {
                logger
                    .error("[GuideNH] [GuideDevelopmentResourcePackWatcher] Failed watching development resources", e);
            }
        }
    }
}
