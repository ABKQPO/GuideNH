package com.hfstudio.guidenh.guide.scene.level;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

/**
 * Per-facing snapshots of AE2 {@link appeng.api.parts.IPart#writeToStream} payloads for a {@link appeng.tile.networking.TileCableBus}
 * side attachment ({@link net.minecraftforge.common.util.ForgeDirection} ordinals {@code 0}–{@code 5}).
 */
public final class ExportedAe2CableBusPartStreams {

    public static final ExportedAe2CableBusPartStreams EMPTY = new ExportedAe2CableBusPartStreams(new byte[6][]);

    private final byte[][] bySideOrdinal;

    public ExportedAe2CableBusPartStreams(byte[][] bySideOrdinal) {
        this.bySideOrdinal = bySideOrdinal != null ? bySideOrdinal : new byte[6][];
    }

    @Nullable
    public byte[] bytesForSideOrdinal(int sideOrdinal) {
        return getSlot(sideOrdinal);
    }

    @Nullable
    public byte[] getSlot(int sideOrdinal) {
        if (sideOrdinal < 0 || sideOrdinal >= 6) {
            return null;
        }
        byte[] b = bySideOrdinal[sideOrdinal];
        return b != null && b.length > 0 ? b : null;
    }

    public boolean isEmpty() {
        for (int i = 0; i < 6; i++) {
            if (getSlot(i) != null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExportedAe2CableBusPartStreams that)) {
            return false;
        }
        return Arrays.deepEquals(bySideOrdinal, that.bySideOrdinal);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(bySideOrdinal);
    }
}
