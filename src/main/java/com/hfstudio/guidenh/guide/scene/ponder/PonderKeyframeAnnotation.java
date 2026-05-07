package com.hfstudio.guidenh.guide.scene.ponder;

import org.jetbrains.annotations.Nullable;

/**
 * Describes a single annotation entry inside a Ponder keyframe (JSON-driven).
 * The {@code type} field controls which fields are meaningful:
 * <ul>
 * <li>{@code diamond} - pos (x,y,z), color, tooltip, alwaysOnTop</li>
 * <li>{@code box} - min (minX,minY,minZ), max (maxX,maxY,maxZ), color, lineWidth, alwaysOnTop</li>
 * <li>{@code line} - from (fromX,fromY,fromZ), to (toX,toY,toZ), color, lineWidth, alwaysOnTop</li>
 * <li>{@code blockface} - block (blockX,blockY,blockZ), color, alwaysOnTop</li>
 * <li>{@code text} - pos (x,y,z), text, color (border), maxWidth (wrap width px), independent, yOffset;
 * optional highlight box via hlMinX/hlMinY/hlMinZ/hlMaxX/hlMaxY/hlMaxZ and highlightColor</li>
 * <li>{@code input} - pos (x,y,z), inputType ("lmb"|"rmb"|"scroll"), modifier ("sneak"|"ctrl"),
 * item (registry ID)</li>
 * </ul>
 */
public class PonderKeyframeAnnotation {

    private String type;

    @Nullable
    private Float x;
    @Nullable
    private Float y;
    @Nullable
    private Float z;

    @Nullable
    private Float minX;
    @Nullable
    private Float minY;
    @Nullable
    private Float minZ;
    @Nullable
    private Float maxX;
    @Nullable
    private Float maxY;
    @Nullable
    private Float maxZ;

    @Nullable
    private Float fromX;
    @Nullable
    private Float fromY;
    @Nullable
    private Float fromZ;
    @Nullable
    private Float toX;
    @Nullable
    private Float toY;
    @Nullable
    private Float toZ;

    @Nullable
    private Integer blockX;
    @Nullable
    private Integer blockY;
    @Nullable
    private Integer blockZ;

    @Nullable
    private String color;
    @Nullable
    private Float lineWidth;
    @Nullable
    private String tooltip;
    @Nullable
    private Boolean alwaysOnTop;
    @Nullable
    private String text;
    @Nullable
    private String inputType;
    @Nullable
    private String modifier;
    @Nullable
    private String item;
    @Nullable
    private Boolean independent;
    @Nullable
    private Integer yOffset;
    @Nullable
    private Integer maxWidth;
    @Nullable
    private Float hlMinX;
    @Nullable
    private Float hlMinY;
    @Nullable
    private Float hlMinZ;
    @Nullable
    private Float hlMaxX;
    @Nullable
    private Float hlMaxY;
    @Nullable
    private Float hlMaxZ;
    @Nullable
    private String highlightColor;

    public String getType() {
        return type != null ? type : "";
    }

    public float getX(float def) {
        return x != null ? x : def;
    }

    public float getY(float def) {
        return y != null ? y : def;
    }

    public float getZ(float def) {
        return z != null ? z : def;
    }

    public float getMinX(float def) {
        return minX != null ? minX : def;
    }

    public float getMinY(float def) {
        return minY != null ? minY : def;
    }

    public float getMinZ(float def) {
        return minZ != null ? minZ : def;
    }

    public float getMaxX(float def) {
        return maxX != null ? maxX : def;
    }

    public float getMaxY(float def) {
        return maxY != null ? maxY : def;
    }

    public float getMaxZ(float def) {
        return maxZ != null ? maxZ : def;
    }

    public float getFromX(float def) {
        return fromX != null ? fromX : def;
    }

    public float getFromY(float def) {
        return fromY != null ? fromY : def;
    }

    public float getFromZ(float def) {
        return fromZ != null ? fromZ : def;
    }

    public float getToX(float def) {
        return toX != null ? toX : def;
    }

    public float getToY(float def) {
        return toY != null ? toY : def;
    }

    public float getToZ(float def) {
        return toZ != null ? toZ : def;
    }

    public int getBlockX(int def) {
        return blockX != null ? blockX : def;
    }

    public int getBlockY(int def) {
        return blockY != null ? blockY : def;
    }

    public int getBlockZ(int def) {
        return blockZ != null ? blockZ : def;
    }

    @Nullable
    public String getColor() {
        return color;
    }

    public float getLineWidth(float def) {
        return lineWidth != null ? lineWidth : def;
    }

    @Nullable
    public String getTooltip() {
        return tooltip;
    }

    public boolean isAlwaysOnTop() {
        return alwaysOnTop != null && alwaysOnTop;
    }

    @Nullable
    public String getText() {
        return text;
    }

    @Nullable
    public String getInputType() {
        return inputType;
    }

    @Nullable
    public String getModifier() {
        return modifier;
    }

    @Nullable
    public String getItem() {
        return item;
    }

    public boolean isIndependent() {
        return independent != null && independent;
    }

    public int getYOffset(int def) {
        return yOffset != null ? yOffset : def;
    }

    /** Maximum text wrap width in pixels for {@code text} annotations. {@code 0} means single-line (no wrap). */
    public int getMaxWidth(int def) {
        return maxWidth != null ? maxWidth : def;
    }

    /**
     * Returns {@code true} when at least one highlight-box coordinate is specified.
     * When true an {@link com.hfstudio.guidenh.guide.scene.annotation.InWorldBoxAnnotation}
     * is created alongside the text annotation.
     */
    public boolean hasHighlight() {
        return hlMinX != null || hlMinY != null || hlMinZ != null || hlMaxX != null || hlMaxY != null || hlMaxZ != null;
    }

    public float getHlMinX(float def) {
        return hlMinX != null ? hlMinX : def;
    }

    public float getHlMinY(float def) {
        return hlMinY != null ? hlMinY : def;
    }

    public float getHlMinZ(float def) {
        return hlMinZ != null ? hlMinZ : def;
    }

    public float getHlMaxX(float def) {
        return hlMaxX != null ? hlMaxX : def;
    }

    public float getHlMaxY(float def) {
        return hlMaxY != null ? hlMaxY : def;
    }

    public float getHlMaxZ(float def) {
        return hlMaxZ != null ? hlMaxZ : def;
    }

    public int parseHighlightColor(int defaultArgb) {
        if (highlightColor == null || highlightColor.isEmpty()) {
            return defaultArgb;
        }
        try {
            return (int) Long.parseLong(
                highlightColor.replace("0x", "")
                    .replace("0X", ""),
                16);
        } catch (NumberFormatException ignored) {
            return defaultArgb;
        }
    }

    public int parseColor(int defaultArgb) {
        if (color == null || color.isEmpty()) {
            return defaultArgb;
        }
        try {
            return (int) Long.parseLong(
                color.replace("0x", "")
                    .replace("0X", ""),
                16);
        } catch (NumberFormatException ignored) {
            return defaultArgb;
        }
    }
}
