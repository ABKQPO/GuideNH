package com.hfstudio.guidenh.compat.ae2;

import org.jetbrains.annotations.Nullable;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Binary wire for {@link Ae2CablePreviewSnapshot} (shared by SNBT Base64, MP batch side-car, and preview store). */
public final class Ae2CablePreviewWireCodec {

    public static final int WIRE_V1 = 1;

    private Ae2CablePreviewWireCodec() {}

    public static byte[] encode(Ae2CablePreviewSnapshot snap) {
        if (snap == null || snap.isEffectivelyEmpty()) {
            return new byte[0];
        }
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(WIRE_V1);
        buf.writeByte(snap.hasCableCore() ? 1 : 0);
        if (snap.hasCableCore()) {
            buf.writeByte((byte) snap.gridCsUnsigned());
            buf.writeInt(snap.sideOut());
        }
        buf.writeBytes(Ae2CableBusPartStreamCodec.pack(snap.sideStreams()));
        byte[] out = new byte[buf.readableBytes()];
        buf.readBytes(out);
        return out;
    }

    public static Ae2CablePreviewSnapshot decode(@Nullable byte[] payload) {
        if (payload == null || payload.length == 0) {
            return Ae2CablePreviewSnapshot.EMPTY;
        }
        ByteBuf buf = Unpooled.wrappedBuffer(payload);
        if (buf.readableBytes() < 2) {
            return Ae2CablePreviewSnapshot.EMPTY;
        }
        int ver = buf.readUnsignedByte();
        if (ver != WIRE_V1) {
            return Ae2CablePreviewSnapshot.EMPTY;
        }
        boolean cableHit = buf.readByte() != 0;
        int csUnsigned = 0;
        int sideOut = 0;
        if (cableHit) {
            if (buf.readableBytes() < 5) {
                return Ae2CablePreviewSnapshot.EMPTY;
            }
            csUnsigned = buf.readByte() & 0xFF;
            sideOut = buf.readInt();
        }
        byte[] remainder = new byte[buf.readableBytes()];
        buf.readBytes(remainder);
        Ae2CableBusSideStreams parts = Ae2CableBusPartStreamCodec.unpack(remainder);
        boolean hasCableSemantics = cableHit;
        return new Ae2CablePreviewSnapshot(hasCableSemantics, csUnsigned, sideOut, parts);
    }
}
