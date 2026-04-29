package com.hfstudio.guidenh.guide.scene.support;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import org.apache.logging.log4j.Logger;

import com.hfstudio.guidenh.config.ModConfig;

public final class GuideDebugLog {

    private GuideDebugLog() {}

    public static boolean isEnabled() {
        return ModConfig.debug.enableDebugMode;
    }

    public static void run(@Nullable Runnable action) {
        if (!isEnabled() || action == null) {
            return;
        }
        action.run();
    }

    public static void runOnce(@Nullable Set<String> onceKeys, @Nullable String key, @Nullable Runnable action) {
        if (!isEnabled() || onceKeys == null || key == null || key.isEmpty() || action == null) {
            return;
        }
        if (onceKeys.add(key)) {
            action.run();
        }
    }

    public static void warn(@Nullable Logger logger, @Nullable String message, Object... args) {
        if (!isEnabled() || logger == null || message == null || message.isEmpty()) {
            return;
        }
        logger.warn(message, args);
    }

    public static void info(@Nullable Logger logger, @Nullable String message, Object... args) {
        if (!isEnabled() || logger == null || message == null || message.isEmpty()) {
            return;
        }
        logger.info(message, args);
    }

    public static void debug(@Nullable Logger logger, @Nullable String message, Object... args) {
        if (!isEnabled() || logger == null || message == null || message.isEmpty()) {
            return;
        }
        logger.debug(message, args);
    }
}
