package com.hfstudio.structurelibexport;

import java.util.Locale;

import org.jetbrains.annotations.Nullable;

public class StructureLibOrientationSpec {

    public static final StructureLibOrientationSpec DEFAULT = new StructureLibOrientationSpec(null, null, null, true);

    @Nullable
    private final String facing;
    @Nullable
    private final String rotation;
    @Nullable
    private final String flip;
    private final boolean defaultOrientation;

    public StructureLibOrientationSpec(@Nullable String facing, @Nullable String rotation, @Nullable String flip,
        boolean defaultOrientation) {
        this.facing = normalize(facing);
        this.rotation = normalize(rotation);
        this.flip = normalize(flip);
        this.defaultOrientation = defaultOrientation;
    }

    public static StructureLibOrientationSpec of(@Nullable String facing, @Nullable String rotation,
        @Nullable String flip) {
        return new StructureLibOrientationSpec(facing, rotation, flip, false);
    }

    @Nullable
    public String getFacing() {
        return facing;
    }

    @Nullable
    public String getRotation() {
        return rotation;
    }

    @Nullable
    public String getFlip() {
        return flip;
    }

    public boolean isDefaultOrientation() {
        return defaultOrientation;
    }

    public String asSuffix() {
        if (defaultOrientation) {
            return "default-orientation";
        }
        return safePart(facing, "default-facing") + "_" + safePart(rotation, "normal") + "_" + safePart(flip, "none");
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT)
            .replace('_', '-');
    }

    private static String safePart(@Nullable String value, String fallback) {
        return value != null ? value : fallback;
    }
}
