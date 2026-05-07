package com.hfstudio.guidenh.guide.scene.ponder;

/**
 * A single block replacement that is applied when a Ponder keyframe becomes active.
 * Parsed from the {@code "blockChanges"} array inside a keyframe JSON object.
 *
 * <p>
 * Example JSON:
 * 
 * <pre>
 * "blockChanges": [
 *   { "x": 1, "y": 1, "z": 2, "block": "minecraft:lit_furnace", "meta": 4 },
 *   { "x": 2, "y": 1, "z": 2, "block": "minecraft:air" }
 * ]
 * </pre>
 *
 * <p>
 * When {@code block} is {@code "minecraft:air"} or omitted the position is cleared to air.
 * All previous keyframe block-changes at the same position are undone before the new ones are
 * applied, so a scene always restores to its initial structure when seeking backwards.
 */
public class PonderKeyframeBlockChange {

    private int x;
    private int y;
    private int z;
    private String block;
    private int meta;

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    /** Registry name of the block to place, e.g. {@code "minecraft:furnace"}. May be null for air. */
    public String getBlock() {
        return block;
    }

    /** Block metadata / damage value. Defaults to {@code 0} when omitted from JSON. */
    public int getMeta() {
        return meta;
    }
}
