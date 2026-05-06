package com.hfstudio.guidenh.compat.ae2;

import org.jetbrains.annotations.Nullable;

/**
 * Unified server-authoritative preview payload for an AE2 {@link appeng.tile.networking.TileCableBus}.
 */
public final class Ae2CablePreviewSnapshot {

    public static final Ae2CablePreviewSnapshot EMPTY = new Ae2CablePreviewSnapshot(
        false,
        0,
        0,
        Ae2CableBusSideStreams.EMPTY);

    private final boolean cableCorePresent;
    private final int gridCsUnsigned;
    private final int sideOut;
    private final Ae2CableBusSideStreams sideStreams;

    public Ae2CablePreviewSnapshot(boolean cableCorePresent, int gridCsUnsigned, int sideOut,
        Ae2CableBusSideStreams sideStreams) {
        this.cableCorePresent = cableCorePresent;
        this.gridCsUnsigned = gridCsUnsigned & 0xFF;
        this.sideOut = sideOut;
        this.sideStreams = sideStreams != null ? sideStreams : Ae2CableBusSideStreams.EMPTY;
    }

    public boolean hasCableCore() {
        return cableCorePresent;
    }

    public int gridCsUnsigned() {
        return gridCsUnsigned;
    }

    public int sideOut() {
        return sideOut;
    }

    public Ae2CableBusSideStreams sideStreams() {
        return sideStreams;
    }

    public boolean isEffectivelyEmpty() {
        return !cableCorePresent && sideStreams.isEmpty();
    }

    @Nullable
    public static Ae2CablePreviewSnapshot mergePreferring(@Nullable Ae2CablePreviewSnapshot primary,
        @Nullable Ae2CablePreviewSnapshot fallback) {
        if (primary != null && !primary.isEffectivelyEmpty()) {
            return primary;
        }
        if (fallback != null && !fallback.isEffectivelyEmpty()) {
            return fallback;
        }
        return null;
    }
}
