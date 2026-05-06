package com.hfstudio.guidenh.compat.ae2;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.level.GuideAe2CableSnbt;
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
 * Captures AE2 {@link PartCable#writeToStream} into structure SNBT ({@link GuideAe2CableSnbt#TAG_ROOT}).
 */
public final class Ae2CableStructureSupport {

    private Ae2CableStructureSupport() {}

    @FunctionalInterface
    public interface ExportTileLookup {

        TileEntity getTile(int x, int y, int z);
    }

    public static final class Ae2CableMpSnapshot {

        private final Map<String, int[]> streams;

        Ae2CableMpSnapshot(Map<String, int[]> streams) {
            this.streams = streams != null ? streams : Collections.emptyMap();
        }

        @Nullable
        public int[] lookup(int dim, int x, int y, int z) {
            return streams.get(mpKey(dim, x, y, z));
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    @Nullable
    public static Ae2CableMpSnapshot tryCreateMpSnapshot(@Nullable World exportWorld, ExportTileLookup lookup, int minX,
        int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!Mods.AE2.isModLoaded() || exportWorld == null || !exportWorld.isRemote || lookup == null) {
            return null;
        }
        if (!isMultiplayerClientNoIntegratedServer()) {
            return null;
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
            return new Ae2CableMpSnapshot(Collections.emptyMap());
        }
        Map<String, int[]> merged = fetchMpStreamsBlocking(dim, coords, 4000L);
        return new Ae2CableMpSnapshot(merged);
    }

    private static Map<String, int[]> fetchMpStreamsBlocking(int dim, List<int[]> coords, long timeoutMsPerBatch) {
        Map<String, int[]> out = new HashMap<>();
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
            long corr = ThreadLocalRandom.current().nextLong();
            GuideNhAe2CableBatchAwait.register(corr);
            GuideNhNetwork.channel()
                .sendToServer(new GuideNhAe2CableBatchRequestMessage(corr, dim, xyz));
            GuideNhAe2CableBatchReplyMessage reply;
            try {
                reply = GuideNhAe2CableBatchAwait.await(corr, timeoutMsPerBatch);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
                break;
            }
            if (reply == null) {
                break;
            }
            byte[] hit = reply.getHit();
            byte[] cs = reply.getCs();
            int[] sideOut = reply.getSideOut();
            if (hit == null || cs == null || sideOut == null || hit.length != n) {
                break;
            }
            for (int i = 0; i < n; i++) {
                if (hit[i] == 0) {
                    continue;
                }
                int[] p = coords.get(start + i);
                out.put(mpKey(dim, p[0], p[1], p[2]), new int[] { cs[i] & 0xFF, sideOut[i] });
            }
        }
        return out;
    }

    private static String mpKey(int dim, int x, int y, int z) {
        return dim + ":" + x + ":" + y + ":" + z;
    }

    /** {@code true} when {@code Minecraft.theIntegratedServer == null} (remote server client). */
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

        if (mpSnapshot != null && exportWorldForAe2 != null) {
            int[] rpc = mpSnapshot.lookup(dim, wx, wy, wz);
            if (rpc != null && rpc.length >= 2) {
                applyTag(structureBlockTag, (byte) rpc[0], rpc[1]);
                return;
            }
        }

        TileEntity workTe = resolveServerCableBusTile(tileEntity, exportWorldForAe2);

        if (!(workTe instanceof TileCableBus resolvedBus)) {
            return;
        }
        if (!(resolvedBus.getPart(ForgeDirection.UNKNOWN) instanceof PartCable cable)) {
            return;
        }

        ByteBuf buf = Unpooled.buffer(5);
        try {
            cable.writeToStream(buf);
        } catch (IOException ignored) {
            return;
        }
        if (buf.readableBytes() < 5) {
            return;
        }
        byte cs = buf.readByte();
        int sideOut = buf.readInt();
        applyTag(structureBlockTag, cs, sideOut);
    }

    private static void applyTag(NBTTagCompound structureBlockTag, byte cs, int sideOut) {
        NBTTagCompound ext = new NBTTagCompound();
        ext.setInteger(GuideAe2CableSnbt.KEY_CS, cs & 0xFF);
        ext.setInteger(GuideAe2CableSnbt.KEY_SIDE_OUT, sideOut);
        structureBlockTag.setTag(GuideAe2CableSnbt.TAG_ROOT, ext);
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
            try {
                Class<?> mcCls = Class.forName("net.minecraft.client.Minecraft");
                Object mc = mcCls.getMethod("getMinecraft").invoke(null);
                Object integratedObj = mcCls.getField("theIntegratedServer").get(mc);
                if (integratedObj instanceof MinecraftServer) {
                    MinecraftServer isrv = (MinecraftServer) integratedObj;
                    sw = isrv.worldServerForDimension(dim);
                    if (sw == null && isrv.worldServers != null) {
                        for (WorldServer w : isrv.worldServers) {
                            if (w != null && w.provider.dimensionId == dim) {
                                sw = w;
                                break;
                            }
                        }
                    }
                }
            } catch (Throwable ignored) {}
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
