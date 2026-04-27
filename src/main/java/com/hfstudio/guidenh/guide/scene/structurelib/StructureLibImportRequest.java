package com.hfstudio.guidenh.guide.scene.structurelib;

import javax.annotation.Nullable;

public class StructureLibImportRequest {

    private final String controller;
    @Nullable
    private final String piece;
    @Nullable
    private final String facing;
    @Nullable
    private final String rotation;
    @Nullable
    private final String flip;
    @Nullable
    private final Integer channel;

    public StructureLibImportRequest(String controller, @Nullable String piece, @Nullable String facing,
        @Nullable String rotation, @Nullable String flip, @Nullable Integer channel) {
        this.controller = requireController(controller);
        this.piece = normalizeOptional(piece);
        this.facing = normalizeOptional(facing);
        this.rotation = normalizeOptional(rotation);
        this.flip = normalizeOptional(flip);
        this.channel = channel;
    }

    public String getController() {
        return controller;
    }

    @Nullable
    public String getPiece() {
        return piece;
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

    @Nullable
    public Integer getChannel() {
        return channel;
    }

    private static String requireController(@Nullable String controller) {
        if (controller == null) {
            throw new IllegalArgumentException("StructureLib controller cannot be null");
        }
        String trimmed = controller.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("StructureLib controller cannot be empty");
        }
        return trimmed;
    }

    @Nullable
    private static String normalizeOptional(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
