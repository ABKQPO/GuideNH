package com.hfstudio.guidenh.compat.ae2;

import java.io.IOException;

import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import appeng.api.parts.IPart;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Packs AE2 cable-bus {@link IPart#writeToStream} bytes per facing ({@code 0}–{@code 5}) for preview wire and MP batch.
 */
public final class Ae2CableBusPartStreamCodec {

    /** Keeps Forge packet chunks bounded; oversized streams are skipped. */
    public static final int MAX_SIDE_PAYLOAD = 8192;

    private Ae2CableBusPartStreamCodec() {}

    @Optional.Method(modid = "appliedenergistics2")
    public static Ae2CableBusSideStreams captureFromBus(TileCableBus bus) {
        byte[][] slots = new byte[6][];
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            int o = dir.ordinal();
            if (o < 0 || o >= 6) {
                continue;
            }
            IPart part = bus.getPart(dir);
            if (part == null) {
                continue;
            }
            ByteBuf pb = Unpooled.buffer();
            try {
                part.writeToStream(pb);
            } catch (IOException ignored) {
                continue;
            }
            int len = pb.readableBytes();
            if (len <= 0 || len > MAX_SIDE_PAYLOAD) {
                continue;
            }
            byte[] chunk = new byte[len];
            pb.readBytes(chunk);
            slots[o] = chunk;
        }
        return new Ae2CableBusSideStreams(slots);
    }

    /**
     * {@code ushort dirMask} then for each set bit {@code o} in ascending {@code o}: {@code ushort len}, {@code len}
     * bytes.
     */
    public static byte[] pack(Ae2CableBusSideStreams streams) {
        if (streams == null || streams.isEmpty()) {
            return new byte[2];
        }
        ByteBuf buf = Unpooled.buffer();
        int mask = 0;
        for (int o = 0; o < 6; o++) {
            byte[] chunk = streams.getSlot(o);
            if (chunk != null && chunk.length > 0) {
                mask |= (1 << o);
            }
        }
        buf.writeShort(mask);
        for (int o = 0; o < 6; o++) {
            if ((mask & (1 << o)) == 0) {
                continue;
            }
            byte[] chunk = streams.getSlot(o);
            int len = chunk != null ? chunk.length : 0;
            buf.writeShort(len);
            if (len > 0) {
                buf.writeBytes(chunk);
            }
        }
        int n = buf.readableBytes();
        byte[] out = new byte[n];
        buf.readBytes(out);
        return out;
    }

    public static Ae2CableBusSideStreams unpack(@Nullable byte[] payload) {
        if (payload == null || payload.length < 2) {
            return Ae2CableBusSideStreams.EMPTY;
        }
        ByteBuf buf = Unpooled.wrappedBuffer(payload);
        int mask = buf.readUnsignedShort();
        byte[][] slots = new byte[6][];
        boolean any = false;
        for (int o = 0; o < 6; o++) {
            if ((mask & (1 << o)) == 0) {
                continue;
            }
            int len = buf.readUnsignedShort();
            if (len < 0 || len > MAX_SIDE_PAYLOAD || buf.readableBytes() < len) {
                return Ae2CableBusSideStreams.EMPTY;
            }
            byte[] chunk = new byte[len];
            buf.readBytes(chunk);
            slots[o] = chunk;
            any = true;
        }
        if (buf.readableBytes() != 0) {
            return Ae2CableBusSideStreams.EMPTY;
        }
        return any ? new Ae2CableBusSideStreams(slots) : Ae2CableBusSideStreams.EMPTY;
    }
}
