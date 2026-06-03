package com.hfstudio.guidenh.guide.scene.ponder;

import org.jetbrains.annotations.Nullable;

import lombok.Getter;

/**
 * A single block replacement that is applied when a Ponder keyframe becomes active.
 * Parsed from the {@code "blockChanges"} array inside a keyframe JSON object.
 *
 * <p>
 * Example JSON:
 *
 * <pre>
 * "blockChanges": [
 *   { "x": 1, "y": 1, "z": 2, "block": "minecraft:lit_furnace", "meta": 4, "particles": true },
 *   { "x": 2, "y": 1, "z": 2, "block": "minecraft:air", "particles": false },
 *   { "x": 0, "y": 1, "z": 1, "block": "minecraft:chest", "meta": 2,
 *     "nbt": "{id:\"Chest\", Items:[{Slot:0b,id:\"minecraft:diamond\",Count:3b,Damage:0s}]}" }
 * ]
 * </pre>
 *
 * <p>
 * When {@code block} is {@code "minecraft:air"} or omitted the position is cleared to air.
 * All previous keyframe block-changes at the same position are undone before the new ones are
 * applied, so a scene always restores to its initial structure when seeking backwards.
 * <p>
 * {@code particles} defaults to {@code true}; set to {@code false} to suppress the brief
 * highlight flash that plays when the block changes during forward playback.
 * <p>
 * {@code nbt} is an optional SNBT string that, when present, is loaded into the placed
 * tile entity (e.g. to pre-fill a chest with items).
 */
@Getter
public class PonderKeyframeBlockChange {

    private int x;
    private int y;
    private int z;
    private String block;
    private int meta;
    @Nullable
    private Boolean particles;
    @Nullable
    private String nbt;

    /**
     * Whether a brief particle/highlight effect should play when this block change is applied
     * during forward playback. Defaults to {@code true} when the JSON field is absent.
     */
    public boolean shouldSpawnParticles() {
        return particles == null || particles;
    }

    /**
     * Optional SNBT tile-entity tag to apply after placing the block.
     * When present the tag is parsed and loaded into the block's {@link TileEntity}.
     */
}
