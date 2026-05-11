package com.hfstudio.guidenh.guide.internal.structure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public static String sanitizePrefix(String prefix) {
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
        String normalized = collapseRepeatedDashes(builder);
        return normalized.isEmpty() ? "structure" : normalized;
    }

    private static String collapseRepeatedDashes(StringBuilder source) {
        StringBuilder collapsed = new StringBuilder(source.length());
        boolean previousDash = false;
        for (int index = 0; index < source.length(); index++) {
            char value = source.charAt(index);
            if (value == '-') {
                if (previousDash) {
                    continue;
                }
                previousDash = true;
            } else {
                previousDash = false;
            }
            collapsed.append(value);
        }
        return collapsed.toString();
    }
}
