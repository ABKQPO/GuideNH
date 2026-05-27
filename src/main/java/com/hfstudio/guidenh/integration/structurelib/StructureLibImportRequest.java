package com.hfstudio.guidenh.integration.structurelib;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

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
    private final StructureLibPreviewSelection previewSelection;

    public StructureLibImportRequest(String controller, @Nullable String piece, @Nullable String facing,
        @Nullable String rotation, @Nullable String flip, @Nullable Integer channel) {
        this(
            controller,
            piece,
            facing,
            rotation,
            flip,
            channel,
            channel != null ? StructureLibPreviewSelection.ofMasterTier(channel)
                : StructureLibPreviewSelection.defaultSelection());
    }

    public StructureLibImportRequest(String controller, @Nullable String piece, @Nullable String facing,
        @Nullable String rotation, @Nullable String flip, @Nullable Integer channel,
        @Nullable StructureLibPreviewSelection previewSelection) {
        this.controller = requireController(controller);
        this.piece = normalizeOptional(piece);
        this.facing = normalizeOptional(facing);
        this.rotation = normalizeOptional(rotation);
        this.flip = normalizeOptional(flip);
        this.channel = channel;
        this.previewSelection = previewSelection != null ? previewSelection
            : StructureLibPreviewSelection.defaultSelection();
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

    public StructureLibPreviewSelection getPreviewSelection() {
        return previewSelection;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StructureLibImportRequest other)) {
            return false;
        }
        return controller.equals(other.controller) && Objects.equals(piece, other.piece)
            && Objects.equals(facing, other.facing)
            && Objects.equals(rotation, other.rotation)
            && Objects.equals(flip, other.flip)
            && Objects.equals(channel, other.channel)
            && previewSelection.equals(other.previewSelection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(controller, piece, facing, rotation, flip, channel, previewSelection);
    }

    public static String requireController(@Nullable String controller) {
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
    public static String normalizeOptional(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
