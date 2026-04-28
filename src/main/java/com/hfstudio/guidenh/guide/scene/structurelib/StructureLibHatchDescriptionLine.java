package com.hfstudio.guidenh.guide.scene.structurelib;

import java.util.Objects;

import javax.annotation.Nullable;

public class StructureLibHatchDescriptionLine {

    public enum Kind {
        HINT_BLOCK,
        VALID_HATCHES
    }

    private final Kind kind;
    private final int hintDot;
    @Nullable
    private final String text;

    private StructureLibHatchDescriptionLine(Kind kind, int hintDot, @Nullable String text) {
        this.kind = Objects.requireNonNull(kind, "kind");
        this.hintDot = Math.max(0, hintDot);
        this.text = normalize(text);
    }

    public static StructureLibHatchDescriptionLine hintBlock(int hintDot) {
        return new StructureLibHatchDescriptionLine(Kind.HINT_BLOCK, hintDot, null);
    }

    public static StructureLibHatchDescriptionLine validHatches(@Nullable String text) {
        return new StructureLibHatchDescriptionLine(Kind.VALID_HATCHES, 0, text);
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

    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
