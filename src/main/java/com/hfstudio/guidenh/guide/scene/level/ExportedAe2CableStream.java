package com.hfstudio.guidenh.guide.scene.level;

/**
 * Snapshot of AE2 cable {@code PartCable.writeToStream} payload carried beside TE {@code nbt} in structure SNBT
 * ({@code guidenh_ae2CableStream_v1}).
 */
public final class ExportedAe2CableStream {

    /** Unsigned {@code cs} byte from AE2 (six facing bits + UNKNOWN powered bit). */
    public final int gridCsUnsigned;

    public final int sideOut;

    public ExportedAe2CableStream(byte gridCsSigned, int sideOut) {
        this.gridCsUnsigned = gridCsSigned & 0xFF;
        this.sideOut = sideOut;
    }
}
