package com.hfstudio.guidenh.compat.ae2;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

/**
 * Per-facing {@link appeng.api.parts.IPart#writeToStream} payloads for a cable bus (ordinals 0–5).
 */
public final class Ae2CableBusSideStreams {

    public static final Ae2CableBusSideStreams EMPTY = new Ae2CableBusSideStreams(new byte[6][]);

    private final byte[][] bySideOrdinal;

    public Ae2CableBusSideStreams(byte[][] bySideOrdinal) {
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
        if (!(o instanceof Ae2CableBusSideStreams that)) {
            return false;
        }
        return Arrays.deepEquals(bySideOrdinal, that.bySideOrdinal);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(bySideOrdinal);
    }
}
