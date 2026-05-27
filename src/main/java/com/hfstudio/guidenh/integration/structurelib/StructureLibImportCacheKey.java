package com.hfstudio.guidenh.integration.structurelib;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

public class StructureLibImportCacheKey {

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

    public StructureLibImportCacheKey(StructureLibImportRequest request) {
        this(
            request.getController(),
            request.getPiece(),
            request.getFacing(),
            request.getRotation(),
            request.getFlip(),
            request.getChannel(),
            request.getPreviewSelection());
    }

    public StructureLibImportCacheKey(String controller, @Nullable String piece, @Nullable String facing,
        @Nullable String rotation, @Nullable String flip, @Nullable Integer channel,
        @Nullable StructureLibPreviewSelection previewSelection) {
        this.controller = StructureLibImportRequest.requireController(controller);
        this.piece = StructureLibImportRequest.normalizeOptional(piece);
        this.facing = StructureLibImportRequest.normalizeOptional(facing);
        this.rotation = StructureLibImportRequest.normalizeOptional(rotation);
        this.flip = StructureLibImportRequest.normalizeOptional(flip);
        this.channel = channel;
        this.previewSelection = previewSelection != null ? previewSelection
            : StructureLibPreviewSelection.defaultSelection();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof StructureLibImportCacheKey other)) {
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
}
