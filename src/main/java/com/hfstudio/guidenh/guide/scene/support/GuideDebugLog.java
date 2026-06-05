package com.hfstudio.guidenh.guide.scene.support;

import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.config.ModConfig;

import cpw.mods.fml.common.FMLLog;

public class GuideDebugLog {

    protected GuideDebugLog() {}

    public static boolean isEnabled() {
        return ModConfig.debug.enableDebugMode;
    }

    public static boolean isDebugEnabled() {
        return isEnabled();
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

    public static void error(@Nullable CharSequence message, Object... args) {
        if (message == null || message.length() <= 0) {
            return;
        }
        FMLLog.getLogger()
            .error(message.toString(), args);
    }

    public static void error(@Nullable Logger ignoredLogger, @Nullable String message, Object... args) {
        error(message, args);
    }

    public static void warn(boolean enabled, @Nullable CharSequence message, Object... args) {
        if (!enabled) {
            return;
        }
        warnAlways(message, args);
    }

    public static void warn(@Nullable CharSequence message, Object... args) {
        if (!isEnabled()) {
            return;
        }
        warnAlways(message, args);
    }

    public static void warnAlways(@Nullable CharSequence message, Object... args) {
        if (message == null || message.length() <= 0) {
            return;
        }
        FMLLog.getLogger()
            .warn(message.toString(), args);
    }

    public static void warn(@Nullable Logger ignoredLogger, @Nullable String message, Object... args) {
        warn(message, args);
    }

    public static void warnAlways(@Nullable Logger ignoredLogger, @Nullable String message, Object... args) {
        warnAlways(message, args);
    }

    public static void info(boolean enabled, @Nullable CharSequence message, Object... args) {
        if (!enabled) {
            return;
        }
        infoAlways(message, args);
    }

    public static void info(@Nullable CharSequence message, Object... args) {
        if (!isEnabled()) {
            return;
        }
        infoAlways(message, args);
    }

    public static void infoAlways(@Nullable CharSequence message, Object... args) {
        if (message == null || message.length() <= 0) {
            return;
        }
        FMLLog.getLogger()
            .info(message.toString(), args);
    }

    public static void info(@Nullable Logger ignoredLogger, @Nullable String message, Object... args) {
        info(message, args);
    }

    public static void infoAlways(@Nullable Logger ignoredLogger, @Nullable String message, Object... args) {
        infoAlways(message, args);
    }

    public static void debug(boolean enabled, @Nullable CharSequence message, Object... args) {
        if (!enabled) {
            return;
        }
        debugAlways(message, args);
    }

    public static void debug(@Nullable CharSequence message, Object... args) {
        if (!isEnabled()) {
            return;
        }
        debugAlways(message, args);
    }

    public static void debugAlways(@Nullable CharSequence message, Object... args) {
        if (message == null || message.length() <= 0) {
            return;
        }
        FMLLog.getLogger()
            .debug(message.toString(), args);
    }

    public static void debug(@Nullable Logger ignoredLogger, @Nullable String message, Object... args) {
        debug(message, args);
    }

    public static void debugAlways(@Nullable Logger ignoredLogger, @Nullable String message, Object... args) {
        debugAlways(message, args);
    }
}
