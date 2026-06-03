package com.hfstudio.guidenh.guide.internal.localization;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jetbrains.annotations.Nullable;

public class GuidePageLangDumpOutputPaths {

    private static final DateTimeFormatter DEFAULT_FOLDER_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private GuidePageLangDumpOutputPaths() {}

    public static Path resolveRequestedOrDefault(@Nullable String rawOutDir, Path workingRoot, LocalDateTime now) {
        if (rawOutDir != null && !rawOutDir.trim()
            .isEmpty()) {
            return workingRoot.resolve(rawOutDir.trim())
                .toAbsolutePath()
                .normalize();
        }

        return workingRoot.resolve(Paths.get("config", "guidenh", "page-lang-dump", DEFAULT_FOLDER_FORMAT.format(now)))
            .toAbsolutePath()
            .normalize();
    }
}
