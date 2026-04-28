package com.hfstudio.guidenh.guide.internal.structure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;

public class GuideStructureFileStore {

    private final Path workingRoot;

    public GuideStructureFileStore(Path workingRoot) {
        this.workingRoot = workingRoot;
    }

    public static GuideStructureFileStore createDefault() {
        return new GuideStructureFileStore(Paths.get(""));
    }

    public Path saveExport(String prefix, String structureText) throws IOException {
        Path path = workingRoot.resolve(
            Paths.get("config", "guidenh", "structures", sanitizePrefix(prefix) + "-" + UUID.randomUUID() + ".snbt"));
        Files.createDirectories(path.getParent());
        Files.write(path, structureText.getBytes(StandardCharsets.UTF_8));
        return path.normalize();
    }

    private static String sanitizePrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return "structure";
        }
        StringBuilder builder = new StringBuilder(prefix.length());
        for (int index = 0; index < prefix.length(); index++) {
            char c = prefix.charAt(index);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                builder.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                builder.append(Character.toLowerCase(c));
            } else {
                builder.append('-');
            }
        }
        String normalized = builder.toString()
            .toLowerCase(Locale.ROOT)
            .replaceAll("-{2,}", "-");
        return normalized.isEmpty() ? "structure" : normalized;
    }
}
