package com.hfstudio.guidenh.compat.ae2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementNbt;
import com.hfstudio.guidenh.network.GuideNhAe2CableBatchAwait;
import com.hfstudio.guidenh.network.GuideNhAe2CableBatchReplyMessage;
import com.hfstudio.guidenh.network.GuideNhAe2CableBatchRequestMessage;
import com.hfstudio.guidenh.network.GuideNhNetwork;

import appeng.parts.networking.PartCable;
import appeng.tile.networking.TileCableBus;
import cpw.mods.fml.common.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Captures AE2 cable-bus preview authority into structure NBT under {@link ServerPreviewSupplementNbt#TAG_ROOT}
 * ({@link Ae2ServerPreviewRegistration#SUPPLEMENT_ID}), and optional multiplayer batch fetch.
 */
public final class Ae2CableStructureSupport {

    private static final Logger LOG = LogManager.getLogger("GuideNH/Ae2Preview");

    private static volatile boolean mpFetchEmptyLogged;

    private Ae2CableStructureSupport() {}

    @FunctionalInterface
    public interface ExportTileLookup {

        TileEntity getTile(int x, int y, int z);
    }

    /** Per-export batch: dim:x:y:z → unified {@link Ae2CablePreviewWireCodec} bytes. */
    public static final class Ae2CableMpSnapshot {

        private final Map<String, byte[]> wireByKey;

        Ae2CableMpSnapshot(Map<String, byte[]> wireByKey) {
            this.wireByKey = wireByKey != null ? wireByKey : Collections.emptyMap();
        }

        public static Ae2CableMpSnapshot empty() {
            return new Ae2CableMpSnapshot(Collections.emptyMap());
        }

        @Nullable
        public byte[] lookupWire(int dim, int x, int y, int z) {
            return wireByKey.get(mpKey(dim, x, y, z));
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static Ae2CableMpSnapshot tryCreateMpSnapshot(@Nullable World exportWorld, ExportTileLookup lookup, int minX,
        int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!Mods.AE2.isModLoaded() || exportWorld == null || !exportWorld.isRemote || lookup == null) {
            return Ae2CableMpSnapshot.empty();
        }
        if (!isMultiplayerClientNoIntegratedServer()) {
            return Ae2CableMpSnapshot.empty();
        }
        int dim = exportWorld.provider.dimensionId;
        List<int[]> coords = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    TileEntity te = lookup.getTile(x, y, z);
                    if (te instanceof TileCableBus) {
                        coords.add(new int[] { x, y, z });
                    }
                }
            }
        }
        if (coords.isEmpty()) {
            return Ae2CableMpSnapshot.empty();
        }
        return fetchMpStreamsBlocking(dim, coords, 4000L);
    }

    private static Ae2CableMpSnapshot fetchMpStreamsBlocking(int dim, List<int[]> coords, long timeoutMsPerBatch) {
        Map<String, byte[]> merged = new HashMap<>();
        int max = GuideNhAe2CableBatchRequestMessage.MAX_POSITIONS;
        for (int start = 0; start < coords.size(); start += max) {
            int end = Math.min(coords.size(), start + max);
            int n = end - start;
            int[] xyz = new int[n * 3];
            for (int i = 0; i < n; i++) {
                int[] p = coords.get(start + i);
                xyz[i * 3] = p[0];
                xyz[i * 3 + 1] = p[1];
                xyz[i * 3 + 2] = p[2];
            }
            long corr = java.util.concurrent.ThreadLocalRandom.current().nextLong();
            GuideNhAe2CableBatchAwait.register(corr);
            GuideNhNetwork.channel()
                .sendToServer(new GuideNhAe2CableBatchRequestMessage(corr, dim, xyz));
            GuideNhAe2CableBatchReplyMessage reply;
            try {
                reply = GuideNhAe2CableBatchAwait.await(corr, timeoutMsPerBatch);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
                logEmptyFetchOnce();
                return Ae2CableMpSnapshot.empty();
            }
            if (reply == null || !reply.isConsistentPayload(n)) {
                logEmptyFetchOnce();
                return Ae2CableMpSnapshot.empty();
            }
            byte[] hit = reply.getHit();
            byte[] cs = reply.getCs();
            int[] sideOut = reply.getSideOut();
            byte[][] partPacked = reply.getPartPacked();
            for (int i = 0; i < n; i++) {
                int[] p = coords.get(start + i);
                String key = mpKey(dim, p[0], p[1], p[2]);
                Ae2CableBusSideStreams sides = Ae2CableBusPartStreamCodec.unpack(partPackedSafe(partPacked, i));
                boolean cableHit = hit[i] != 0;
                int csUnsigned = cs[i] & 0xFF;
                int sideO = sideOut[i];
                Ae2CablePreviewSnapshot snap = new Ae2CablePreviewSnapshot(cableHit, csUnsigned, sideO, sides);
                byte[] wire = Ae2CablePreviewWireCodec.encode(snap);
                if (wire.length > 0) {
                    merged.put(key, wire);
                }
            }
        }
        return new Ae2CableMpSnapshot(merged);
    }

    private static void logEmptyFetchOnce() {
        if (!mpFetchEmptyLogged) {
            mpFetchEmptyLogged = true;
            LOG.info("AE2 preview MP batch unavailable or malformed; exporting cable supplements from local/client only.");
        }
    }

    private static byte[] partPackedSafe(@Nullable byte[][] partPacked, int i) {
        if (partPacked == null || i < 0 || i >= partPacked.length) {
            return new byte[0];
        }
        return partPacked[i] != null ? partPacked[i] : new byte[0];
    }

    private static String mpKey(int dim, int x, int y, int z) {
        return dim + ":" + x + ":" + y + ":" + z;
    }

    private static boolean isMultiplayerClientNoIntegratedServer() {
        try {
            Class<?> mcCls = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcCls.getMethod("getMinecraft").invoke(null);
            Object integrated = mcCls.getField("theIntegratedServer").get(mc);
            return integrated == null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** For {@link Ae2BaseTileNetworkStructureSupport}: same heuristic as cable MP snapshot gating. */
    static boolean isMultiplayerClientNoIntegratedServerPublic() {
        return isMultiplayerClientNoIntegratedServer();
    }

    @Nullable
    static WorldServer tryIntegratedServerWorld(int dim) {
        try {
            Class<?> mcCls = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcCls.getMethod("getMinecraft").invoke(null);
            Object integratedObj = mcCls.getField("theIntegratedServer").get(mc);
            if (integratedObj instanceof MinecraftServer) {
                MinecraftServer isrv = (MinecraftServer) integratedObj;
                WorldServer sw = isrv.worldServerForDimension(dim);
                if (sw != null) {
                    return sw;
                }
                if (isrv.worldServers != null) {
                    for (WorldServer w : isrv.worldServers) {
                        if (w != null && w.provider.dimensionId == dim) {
                            return w;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void attachCableStreamToExport(@Nullable TileEntity tileEntity, NBTTagCompound structureBlockTag) {
        attachCableStreamToExport(tileEntity, structureBlockTag, null, null);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void attachCableStreamToExport(@Nullable TileEntity tileEntity, NBTTagCompound structureBlockTag,
        @Nullable World exportWorldForAe2) {
        attachCableStreamToExport(tileEntity, structureBlockTag, exportWorldForAe2, null);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void attachCableStreamToExport(@Nullable TileEntity tileEntity, NBTTagCompound structureBlockTag,
        @Nullable World exportWorldForAe2, @Nullable Ae2CableMpSnapshot mpSnapshot) {
        if (tileEntity == null || !(tileEntity instanceof TileCableBus)) {
            return;
        }
        int wx = tileEntity.xCoord;
        int wy = tileEntity.yCoord;
        int wz = tileEntity.zCoord;
        int dim = exportWorldForAe2 != null ? exportWorldForAe2.provider.dimensionId : Integer.MIN_VALUE;

        Ae2CablePreviewSnapshot local = captureSnapshotFromWorldTile(tileEntity, exportWorldForAe2);
        byte[] rpcWire = null;
        if (mpSnapshot != null && exportWorldForAe2 != null) {
            rpcWire = mpSnapshot.lookupWire(dim, wx, wy, wz);
        }
        Ae2CablePreviewSnapshot chosen = Ae2CablePreviewSnapshot.mergePreferring(
            rpcWire != null && rpcWire.length > 0 ? Ae2CablePreviewWireCodec.decode(rpcWire) : null,
            local);
        writeSnapshotToStructure(structureBlockTag, chosen);
    }

    @Optional.Method(modid = "appliedenergistics2")
    @Nullable
    private static Ae2CablePreviewSnapshot captureSnapshotFromWorldTile(TileEntity tileEntity,
        @Nullable World exportWorldForAe2) {
        TileEntity workTe = resolveServerCableBusTile(tileEntity, exportWorldForAe2);
        if (!(workTe instanceof TileCableBus resolvedBus)) {
            return null;
        }
        Ae2CableBusSideStreams parts = Ae2CableBusPartStreamCodec.captureFromBus(resolvedBus);
        if (!(resolvedBus.getPart(ForgeDirection.UNKNOWN) instanceof PartCable cable)) {
            return parts.isEmpty() ? null : new Ae2CablePreviewSnapshot(false, 0, 0, parts);
        }
        ByteBuf buf = Unpooled.buffer(5);
        try {
            cable.writeToStream(buf);
        } catch (IOException ignored) {
            return parts.isEmpty() ? null : new Ae2CablePreviewSnapshot(false, 0, 0, parts);
        }
        if (buf.readableBytes() < 5) {
            return parts.isEmpty() ? null : new Ae2CablePreviewSnapshot(false, 0, 0, parts);
        }
        byte cs = buf.readByte();
        int sideOut = buf.readInt();
        return new Ae2CablePreviewSnapshot(true, cs & 0xFF, sideOut, parts);
    }

    private static void writeSnapshotToStructure(NBTTagCompound structureBlockTag, @Nullable Ae2CablePreviewSnapshot snap) {
        if (snap == null || snap.isEffectivelyEmpty()) {
            ServerPreviewSupplementNbt.removeSupplement(structureBlockTag, Ae2ServerPreviewRegistration.SUPPLEMENT_ID);
            return;
        }
        byte[] wire = Ae2CablePreviewWireCodec.encode(snap);
        ServerPreviewSupplementNbt.putSupplement(structureBlockTag, Ae2ServerPreviewRegistration.SUPPLEMENT_ID, wire);
    }

    private static TileEntity resolveServerCableBusTile(TileEntity clientTe, @Nullable World exportWorld) {
        if (clientTe == null || exportWorld == null || !exportWorld.isRemote) {
            return clientTe;
        }
        int dim = exportWorld.provider.dimensionId;
        int x = clientTe.xCoord;
        int y = clientTe.yCoord;
        int z = clientTe.zCoord;

        WorldServer sw = null;

        MinecraftServer ms = MinecraftServer.getServer();
        if (ms != null) {
            sw = ms.worldServerForDimension(dim);
            if (sw == null && ms.worldServers != null) {
                for (WorldServer w : ms.worldServers) {
                    if (w != null && w.provider.dimensionId == dim) {
                        sw = w;
                        break;
                    }
                }
            }
        }

        if (sw == null) {
            sw = tryIntegratedServerWorld(dim);
        }

        if (sw == null) {
            return clientTe;
        }

        TileEntity srv = sw.getTileEntity(x, y, z);
        if (srv instanceof TileCableBus) {
            return srv;
        }
        return clientTe;
    }
}
