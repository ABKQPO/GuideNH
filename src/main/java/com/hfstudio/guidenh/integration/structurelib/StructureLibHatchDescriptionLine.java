package com.hfstudio.guidenh.integration.structurelib;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

public class StructureLibHatchDescriptionLine {

    private static final Map<String, Kind> KINDS = new LinkedHashMap<>();

    public static final Kind HINT_BLOCK = registerKind("guidenh:structurelib_hint_block");
    public static final Kind VALID_HATCHES = registerKind("guidenh:structurelib_valid_hatches");

    private final Kind kind;
    private final int hintDot;
    @Nullable
    private final String text;

    public StructureLibHatchDescriptionLine(Kind kind, int hintDot, @Nullable String text) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.hintDot = Math.max(0, hintDot);
        this.text = normalize(text);
    }

    public static StructureLibHatchDescriptionLine hintBlock(int hintDot) {
        return new StructureLibHatchDescriptionLine(HINT_BLOCK, hintDot, null);
    }

    public static StructureLibHatchDescriptionLine validHatches(@Nullable String text) {
        return new StructureLibHatchDescriptionLine(VALID_HATCHES, 0, text);
    }

    public static StructureLibHatchDescriptionLine of(Kind kind, int hintDot, @Nullable String text) {
        return new StructureLibHatchDescriptionLine(kind, hintDot, text);
    }

    public static synchronized Kind registerKind(String id) {
        String normalizedId = normalizeKindId(id);
        Kind existing = KINDS.get(normalizedId);
        if (existing != null) {
            return existing;
        }
        Kind kind = new Kind(normalizedId);
        KINDS.put(normalizedId, kind);
        return kind;
    }

    @Nullable
    public static synchronized Kind kind(@Nullable String id) {
        String normalizedId = normalizeKindIdOrNull(id);
        return normalizedId != null ? KINDS.get(normalizedId) : null;
    }

    public static synchronized Map<String, Kind> kinds() {
        return Map.copyOf(new LinkedHashMap<>(KINDS));
    }

    public Kind getKind() {
        return kind;
    }

    public int getHintDot() {
        return hintDot;
    }

    @Nullable
    public String getText() {
        return text;
    }

    public static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String normalizeKindId(String id) {
        String normalizedId = normalizeKindIdOrNull(id);
        if (normalizedId == null) {
            throw new IllegalArgumentException("id");
        }
        return normalizedId;
    }

    @Nullable
    public static String normalizeKindIdOrNull(@Nullable String id) {
        String normalized = normalize(id);
        return normalized != null ? normalized : null;
    }

    public static class Kind {

        private final String id;

        public Kind(String id) {
            this.id = normalizeKindId(id);
        }

        public String id() {
            return id;
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
