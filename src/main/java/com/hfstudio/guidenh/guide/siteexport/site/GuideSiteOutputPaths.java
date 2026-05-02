package com.hfstudio.guidenh.guide.siteexport.site;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jetbrains.annotations.Nullable;

public final class GuideSiteOutputPaths {

    private static final DateTimeFormatter DEFAULT_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private GuideSiteOutputPaths() {}

    public static Path resolveRequestedOrDefault(@Nullable String rawOutDir, Path workingRoot, LocalDateTime now) {
        if (rawOutDir != null && !rawOutDir.trim()
            .isEmpty()) {
            return workingRoot.resolve(rawOutDir.trim())
                .toAbsolutePath()
                .normalize();
        }

        return workingRoot.resolve(Paths.get("config", "guidenh", "site-export", DEFAULT_FOLDER_FORMAT.format(now)))
            .toAbsolutePath()
            .normalize();
    }
}
