package com.hfstudio.guidenh.compat.ae2;

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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.compat.Mods;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementNbt;
import com.hfstudio.guidenh.network.GuideNhAe2BaseTileNetworkBatchAwait;
import com.hfstudio.guidenh.network.GuideNhAe2BaseTileNetworkBatchReplyMessage;
import com.hfstudio.guidenh.network.GuideNhAe2BaseTileNetworkBatchRequestMessage;
import com.hfstudio.guidenh.network.GuideNhNetwork;

import appeng.tile.AEBaseTile;
import cpw.mods.fml.common.Optional;

/**
 * Type2 MP snapshot + structure NBT attach for {@link Ae2BaseTileNetworkStreamPreview#SUPPLEMENT_ID}, mirroring
 * {@link Ae2CableStructureSupport}.
 */
public final class Ae2BaseTileNetworkStructureSupport {

    private static final Logger LOG = LogManager.getLogger("GuideNH/Ae2Preview");

    private static volatile boolean mpFetchEmptyLoggedBaseTile;

    private Ae2BaseTileNetworkStructureSupport() {}

    @FunctionalInterface
    public interface ExportTileLookup {

        TileEntity getTile(int x, int y, int z);
    }

    /** Per-export: dim:x:y:z → raw {@code X} description bytes. */
    public static final class Ae2BaseTileNetworkMpSnapshot {

        private final Map<String, byte[]> xpByKey;

        Ae2BaseTileNetworkMpSnapshot(Map<String, byte[]> xpByKey) {
            this.xpByKey = xpByKey != null ? xpByKey : Collections.emptyMap();
        }

        public static Ae2BaseTileNetworkMpSnapshot empty() {
            return new Ae2BaseTileNetworkMpSnapshot(Collections.emptyMap());
        }

        @Nullable
        public byte[] lookupXpPayload(int dim, int x, int y, int z) {
            return xpByKey.get(mpKey(dim, x, y, z));
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static Ae2BaseTileNetworkMpSnapshot tryCreateMpSnapshot(@Nullable World exportWorld,
        ExportTileLookup lookup, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        if (!Mods.AE2.isModLoaded() || exportWorld == null || !exportWorld.isRemote || lookup == null) {
            return Ae2BaseTileNetworkMpSnapshot.empty();
        }
        if (!Ae2CableStructureSupport.isMultiplayerClientNoIntegratedServerPublic()) {
            return Ae2BaseTileNetworkMpSnapshot.empty();
        }
        int dim = exportWorld.provider.dimensionId;
        List<int[]> coords = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    TileEntity te = lookup.getTile(x, y, z);
                    if (Ae2BaseTileNetworkStreamPreview.eligible(te)) {
                        coords.add(new int[] { x, y, z });
                    }
                }
            }
        }
        if (coords.isEmpty()) {
            return Ae2BaseTileNetworkMpSnapshot.empty();
        }
        return fetchMpXpBlocking(dim, coords, 4000L);
    }

    private static Ae2BaseTileNetworkMpSnapshot fetchMpXpBlocking(int dim, List<int[]> coords, long timeoutMsPerBatch) {
        Map<String, byte[]> merged = new HashMap<>();
        int max = GuideNhAe2BaseTileNetworkBatchRequestMessage.MAX_POSITIONS;
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
            long corr = java.util.concurrent.ThreadLocalRandom.current()
                .nextLong();
            GuideNhAe2BaseTileNetworkBatchAwait.register(corr);
            GuideNhNetwork.channel()
                .sendToServer(new GuideNhAe2BaseTileNetworkBatchRequestMessage(corr, dim, xyz));
            GuideNhAe2BaseTileNetworkBatchReplyMessage reply;
            try {
                reply = GuideNhAe2BaseTileNetworkBatchAwait.await(corr, timeoutMsPerBatch);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
                logEmptyFetchOnceBaseTile();
                return Ae2BaseTileNetworkMpSnapshot.empty();
            }
            if (reply == null || !reply.isConsistentPayload(n)) {
                logEmptyFetchOnceBaseTile();
                return Ae2BaseTileNetworkMpSnapshot.empty();
            }
            byte[][] payloads = reply.getXpPayloads();
            for (int i = 0; i < n; i++) {
                int[] p = coords.get(start + i);
                String key = mpKey(dim, p[0], p[1], p[2]);
                byte[] chunk = payloadsSafe(payloads, i);
                if (chunk != null && chunk.length > 0) {
                    merged.put(key, chunk);
                }
            }
        }
        return new Ae2BaseTileNetworkMpSnapshot(merged);
    }

    private static byte[] payloadsSafe(byte[][] payloads, int i) {
        if (payloads == null || i < 0 || i >= payloads.length) {
            return null;
        }
        return payloads[i];
    }

    private static void logEmptyFetchOnceBaseTile() {
        if (!mpFetchEmptyLoggedBaseTile) {
            mpFetchEmptyLoggedBaseTile = true;
            LOG.info(
                "AE2 BaseTile-network preview MP batch unavailable or malformed; exporting ae_base_tile_network supplements from local/client only.");
        }
    }

    private static String mpKey(int dim, int x, int y, int z) {
        return dim + ":" + x + ":" + y + ":" + z;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void attachBaseTileNetworkToExport(@Nullable TileEntity tileEntity, NBTTagCompound structureBlockTag) {
        attachBaseTileNetworkToExport(tileEntity, structureBlockTag, null, null);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void attachBaseTileNetworkToExport(@Nullable TileEntity tileEntity, NBTTagCompound structureBlockTag,
        @Nullable World exportWorldForAe2) {
        attachBaseTileNetworkToExport(tileEntity, structureBlockTag, exportWorldForAe2, null);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void attachBaseTileNetworkToExport(@Nullable TileEntity tileEntity, NBTTagCompound structureBlockTag,
        @Nullable World exportWorldForAe2, @Nullable Ae2BaseTileNetworkMpSnapshot mpSnapshot) {
        if (tileEntity == null || !Ae2BaseTileNetworkStreamPreview.eligible(tileEntity)) {
            return;
        }
        if (!(tileEntity instanceof AEBaseTile aeTile)) {
            return;
        }
        int wx = tileEntity.xCoord;
        int wy = tileEntity.yCoord;
        int wz = tileEntity.zCoord;
        int dim = exportWorldForAe2 != null ? exportWorldForAe2.provider.dimensionId : Integer.MIN_VALUE;

        byte[] local = captureXpFromWorldTile(aeTile, exportWorldForAe2);
        byte[] rpcXp = null;
        if (mpSnapshot != null && exportWorldForAe2 != null) {
            rpcXp = mpSnapshot.lookupXpPayload(dim, wx, wy, wz);
        }
        byte[] chosen = null;
        String src = "none";
        if (rpcXp != null && rpcXp.length > 0) {
            chosen = rpcXp;
            src = "rpc";
        } else if (local != null && local.length > 0) {
            chosen = local;
            src = "local";
        }

        writeXpToStructure(structureBlockTag, chosen);
    }

    @Nullable
    @Optional.Method(modid = "appliedenergistics2")
    private static byte[] captureXpFromWorldTile(AEBaseTile clientOrLocalTile, @Nullable World exportWorldForAe2) {
        TileEntity workTe = resolveServerAeBaseTileTile(clientOrLocalTile, exportWorldForAe2);
        if (!(workTe instanceof AEBaseTile ae)) {
            return null;
        }
        return Ae2BaseTileNetworkStreamPreview.captureAuthoritativeXPayload(ae);
    }

    private static void writeXpToStructure(NBTTagCompound structureBlockTag, @Nullable byte[] xp) {
        if (xp == null || xp.length == 0) {
            ServerPreviewSupplementNbt.removeSupplement(structureBlockTag,
                Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID);
            return;
        }
        ServerPreviewSupplementNbt.putSupplement(structureBlockTag, Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID,
            xp);
    }

    private static TileEntity resolveServerAeBaseTileTile(TileEntity clientTe, @Nullable World exportWorld) {
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
            sw = Ae2CableStructureSupport.tryIntegratedServerWorld(dim);
        }

        if (sw == null) {
            return clientTe;
        }

        TileEntity srv = sw.getTileEntity(x, y, z);
        if (srv != null && Ae2BaseTileNetworkStreamPreview.eligible(srv)) {
            return srv;
        }
        return clientTe;
    }
}
