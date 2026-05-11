package com.hfstudio.structurelibexport;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StructureLibExportFileNamer {

    public static final int MAX_BASE_LENGTH = 120;

    private final Set<String> usedNames = new HashSet<>();

    public Path resolve(Path outputDirectory, StructureLibExportTaskSpec task) {
        String baseName = constrainBaseName(sanitize(buildBaseName(task)));
        String candidate = baseName + ".png";
        int collision = 2;
        while (!usedNames.add(candidate.toLowerCase(Locale.ROOT))) {
            candidate = baseName + "_" + collision + ".png";
            collision++;
        }
        return outputDirectory.resolve(candidate);
    }

    public static String constrainBaseName(String value) {
        String baseName = value != null ? value : "";
        if (baseName.length() > MAX_BASE_LENGTH) {
            baseName = baseName.substring(0, MAX_BASE_LENGTH);
        }
        baseName = sanitize(baseName);
        return baseName.isEmpty() ? "structurelib-export" : baseName;
    }

    public static String sanitize(String value) {
        StringBuilder builder = new StringBuilder();
        String source = value != null ? value : "";
        for (int i = 0; i < source.length(); i++) {
            char ch = source.charAt(i);
            if (ch < 32 || ch == '<'
                || ch == '>'
                || ch == ':'
                || ch == '"'
                || ch == '/'
                || ch == '\\'
                || ch == '|'
                || ch == '?'
                || ch == '*') {
                builder.append('_');
            } else {
                builder.append(ch);
            }
        }
        String sanitized = builder.toString()
            .trim();
        while (sanitized.endsWith(".") || sanitized.endsWith(" ")) {
            sanitized = sanitized.substring(0, sanitized.length() - 1);
        }
        if (isReservedDeviceName(sanitized)) {
            return "_" + sanitized;
        }
        return sanitized;
    }

    private static String buildBaseName(StructureLibExportTaskSpec task) {
        StringBuilder builder = new StringBuilder(
            task.getController()
                .getDisplayName());
        builder.append("_tier-")
            .append(task.getTier());
        for (Map.Entry<String, Integer> entry : task.getChannels()
            .entrySet()) {
            builder.append("_")
                .append(entry.getKey())
                .append("-")
                .append(entry.getValue());
        }
        if (task.getExplicitLayer() != null) {
            builder.append("_layer-")
                .append(task.getExplicitLayer());
        } else if (!"all".equalsIgnoreCase(task.getLayerExpression())) {
            builder.append("_layers-")
                .append(task.getLayerExpression());
        }
        if (!task.getOrientation()
            .isDefaultOrientation()) {
            builder.append("_")
                .append(
                    task.getOrientation()
                        .asSuffix());
        }
        builder.append("_")
            .append(
                task.getView()
                    .getName());
        return builder.toString();
    }

    private static boolean isReservedDeviceName(String value) {
        String normalized = value.toUpperCase(Locale.ROOT);
        int dot = normalized.indexOf('.');
        if (dot >= 0) {
            normalized = normalized.substring(0, dot);
        }
        return "CON".equals(normalized) || "PRN".equals(normalized)
            || "AUX".equals(normalized)
            || "NUL".equals(normalized)
            || normalized.matches("COM[1-9]")
            || normalized.matches("LPT[1-9]");
    }
}
