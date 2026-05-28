package com.hfstudio.guidenh.integration.ae2;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.level.GuidebookPreviewWorld;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportBlockContext;
import com.hfstudio.guidenh.guide.scene.snapshot.ExportSession;
import com.hfstudio.guidenh.guide.scene.snapshot.GuidebookLevelStructureExportAccess;
import com.hfstudio.guidenh.guide.scene.snapshot.ServerPreviewSupplementNbt;
import com.hfstudio.guidenh.guide.scene.snapshot.StructureExportAccess;
import com.hfstudio.guidenh.guide.scene.snapshot.StructureExportPipeline;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockStatsStackResolver;

import appeng.api.AEApi;
import appeng.api.networking.IGridHost;
import appeng.api.parts.IFacadePart;
import appeng.api.parts.IPart;
import appeng.api.parts.PartItemStack;
import appeng.api.util.AECableType;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.parts.CableBusContainer;
import appeng.parts.networking.PartCable;
import appeng.tile.AEBaseTile;
import appeng.tile.crafting.TileCraftingTile;
import appeng.tile.networking.TileCableBus;
import appeng.tile.qnb.TileQuantumBridge;
import appeng.tile.spatial.TileSpatialPylon;
import cpw.mods.fml.common.Optional;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * AE2 guide preview: applies server-authoritative AE2 preview bytes from {@link GuidebookLevel#previewAuthorityStore()}
 * ({@link Ae2ServerPreviewRegistration#SUPPLEMENT_ID} cable bus; {@link Ae2BaseTileNetworkStreamPreview#SUPPLEMENT_ID}
 * other {@link AEBaseTile}), merged with locally inferred cable facings where applicable.
 */
public class Ae2Helpers {

    /** Low six bits of PartCable stream {@code cs}: {@link ForgeDirection#VALID_DIRECTIONS} only. */
    private static final int CS_DIRECTION_MASK = 0x3F;
    private static final String CABLE_BUS_TILE_ID = "BlockCableBus";
    private static final String CABLE_BUS_BLOCK_ID = "appliedenergistics2:tile.BlockCableBus";

    private Ae2Helpers() {}

    /**
     * Whether {@link World#markBlockForUpdate} must not reapply
     * {@link TileEntity#getDescriptionPacket}
     * for this TE inside the guidebook preview world:
     * {@link #prepare(GuidebookLevel)}
     * already merged server-authoritative preview bytes ({@link Ae2ServerPreviewRegistration#SUPPLEMENT_ID} /
     * {@link Ae2BaseTileNetworkStreamPreview#SUPPLEMENT_ID}). Vanilla description resync rebuilds payloads from an
     * inert preview grid / proxy and overrides that state.
     */
    @Optional.Method(modid = "appliedenergistics2")
    @Nullable
    public static Block resolvePonderBlock(@Nullable NBTTagCompound tileTag) {
        if (tileTag == null || !CABLE_BUS_TILE_ID.equals(tileTag.getString("id"))) {
            return null;
        }
        Block block = (Block) Block.blockRegistry.getObject(CABLE_BUS_BLOCK_ID);
        return block != null && block != Blocks.air ? block : null;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static Map<String, byte[]> capturePonderPreviewSupplements(GuidebookLevel level, int x, int y, int z,
        @Nullable Block block, int meta) {
        if (level == null || block == null || block == Blocks.air) {
            return Map.of();
        }
        TileEntity tileEntity = level.getTileEntity(x, y, z);
        if (!(tileEntity instanceof TileCableBus)) {
            return Map.of();
        }
        NBTTagCompound structureBlockTag = new NBTTagCompound();
        structureBlockTag.setIntArray("pos", new int[] { x, y, z });
        StructureExportAccess access = new GuidebookLevelStructureExportAccess(level);
        ExportSession session = new ExportSession(access, x, y, z, x, y, z, 1, 1, 1);
        StructureExportPipeline.beginExport(session);
        try {
            StructureExportPipeline
                .contributeBlock(new ExportBlockContext(session, x, y, z, block, meta, tileEntity, structureBlockTag));
        } finally {
            StructureExportPipeline.endExport(session);
        }
        return readPreviewSupplements(structureBlockTag);
    }

    private static Map<String, byte[]> readPreviewSupplements(@Nullable NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(ServerPreviewSupplementNbt.TAG_ROOT, 10)) {
            return Map.of();
        }
        NBTTagCompound root = tag.getCompoundTag(ServerPreviewSupplementNbt.TAG_ROOT);
        Map<String, byte[]> result = new LinkedHashMap<>();
        for (String supplementId : root.func_150296_c()) {
            byte[] payload = ServerPreviewSupplementNbt.readSupplement(tag, supplementId);
            if (payload != null && payload.length > 0) {
                result.put(supplementId, payload);
            }
        }
        return result.isEmpty() ? Map.of() : result;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static boolean suppressMarkBlockForUpdateDescriptionResync(@Nullable TileEntity te, GuidebookLevel level) {
        if (te == null || level == null) {
            return false;
        }
        if (te instanceof TileCableBus) {
            return true;
        }
        if (te instanceof AEBaseTile) {
            long posKey = GuidebookLevel.packPos(te.xCoord, te.yCoord, te.zCoord);
            byte[] blob = level.previewAuthorityStore()
                .get(posKey, Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID);
            return blob != null && blob.length > 0;
        }
        return false;
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void prepare(GuidebookLevel level) {
        for (TileEntity te : level.getTileEntities()) {
            if (te instanceof TileCraftingTile craftingTile) {
                initCraftingTileValidSides(craftingTile);
            } else if (te instanceof TileQuantumBridge qnb) {
                applyNonCableBaseTilePreview(qnb, level);
                initQuantumBridgeValidSides(qnb);
            } else if (te instanceof AEBaseTile aeTile && !(te instanceof TileCableBus)) {
                initProxyOrientedValidSides(aeTile);
                applyNonCableBaseTilePreview(aeTile, level);
            }
        }
        for (TileEntity te : level.getTileEntities()) {
            CableBusContainer container = resolveCableContainer(te);
            if (container != null) {
                syncCableBusConnections(container, level);
                syncCableBusSidePartStreams(container, level);
            }
        }
        if (level.getOrCreateFakeWorld() instanceof GuidebookPreviewWorld previewWorld) {
            previewWorld.syncLoadedTileEntities(level.getTileEntities());
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static void initQuantumBridgeValidSides(TileQuantumBridge qnb) {
        if (!qnb.isFormed()) {
            return;
        }
        AENetworkProxy proxy;
        try {
            proxy = qnb.getProxy();
        } catch (Throwable ignored) {
            return;
        }
        if (proxy == null) {
            return;
        }
        if (qnb.isCorner() || isQuantumLinkCenter(qnb)) {
            try {
                proxy.setValidSides(qnb.getConnections());
            } catch (Throwable ignored) {}
        } else {
            proxy.setValidSides(EnumSet.allOf(ForgeDirection.class));
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static boolean isQuantumLinkCenter(TileQuantumBridge qnb) {
        for (Block link : AEApi.instance()
            .definitions()
            .blocks()
            .quantumLink()
            .maybeBlock()
            .asSet()) {
            return qnb.getBlockType() == link;
        }
        return false;
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static void initCraftingTileValidSides(TileCraftingTile craftingTile) {
        try {
            craftingTile.updateMeta(true);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void initProxyOrientedValidSides(AEBaseTile aeTile) {
        if (!aeTile.canBeRotated() || aeTile.getForward() == ForgeDirection.UNKNOWN) {
            return;
        }
        if (!(aeTile instanceof IGridProxyable proxyable)) {
            return;
        }
        AENetworkProxy proxy;
        try {
            proxy = proxyable.getProxy();
        } catch (Throwable ignored) {
            return;
        }
        if (proxy == null || !proxy.getConnectableSides()
            .isEmpty()) {
            return;
        }
        try {
            aeTile.setOrientation(aeTile.getForward(), aeTile.getUp());
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncCableBusConnections(TileCableBus cableBusTile, GuidebookLevel level) {
        syncCableBusConnections(cableBusTile.getCableBus(), level);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncCableBusConnections(CableBusContainer container, GuidebookLevel level) {
        if (!(container.getPart(ForgeDirection.UNKNOWN) instanceof PartCable cable)) {
            return;
        }

        int csDirections = computeCableConnectionMask(container, level);
        TileEntity tile = container.getTile();
        long posKey = GuidebookLevel.packPos(tile.xCoord, tile.yCoord, tile.zCoord);
        byte[] raw = level.previewAuthorityStore()
            .get(posKey, Ae2ServerPreviewRegistration.SUPPLEMENT_ID);
        Ae2CablePreviewSnapshot snap = raw != null ? Ae2CablePreviewWireCodec.decode(raw)
            : Ae2CablePreviewSnapshot.EMPTY;

        int poweredMask = 1 << ForgeDirection.UNKNOWN.ordinal();
        int csOut;
        int sideOut;
        if (snap.hasCableCore()) {
            csOut = (snap.gridCsUnsigned() & ~CS_DIRECTION_MASK) | (csDirections & CS_DIRECTION_MASK);
            sideOut = snap.sideOut();
            if (sideOut != 0 && (csOut & poweredMask) == 0) {
                csOut |= poweredMask;
            }
        } else {
            csOut = csDirections;
            sideOut = 0;
        }

        ByteBuf buf = Unpooled.buffer(5);
        buf.writeByte((byte) csOut);
        buf.writeInt(sideOut);
        try {
            cable.readFromStream(buf);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncCableBusSidePartStreams(TileCableBus cableBusTile, GuidebookLevel level) {
        syncCableBusSidePartStreams(cableBusTile.getCableBus(), level);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncCableBusSidePartStreams(CableBusContainer container, GuidebookLevel level) {
        TileEntity tile = container.getTile();
        long posKey = GuidebookLevel.packPos(tile.xCoord, tile.yCoord, tile.zCoord);
        byte[] raw = level.previewAuthorityStore()
            .get(posKey, Ae2ServerPreviewRegistration.SUPPLEMENT_ID);
        if (raw == null || raw.length == 0) {
            return;
        }
        Ae2CablePreviewSnapshot snap = Ae2CablePreviewWireCodec.decode(raw);
        if (snap.sideStreams()
            .isEmpty()) {
            return;
        }

        NBTTagCompound baseline = new NBTTagCompound();
        container.writeToNBT(baseline);

        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            byte[] blob = snap.sideStreams()
                .bytesForSideOrdinal(dir.ordinal());
            if (blob == null || blob.length == 0) {
                continue;
            }
            if (container.getPart(dir) == null) {
                continue;
            }

            container.readFromNBT((NBTTagCompound) baseline.copy());
            tile.validate();
            IPart part = container.getPart(dir);
            if (part == null) {
                continue;
            }
            ByteBuf buf = Unpooled.wrappedBuffer(blob);
            try {
                part.readFromStream(buf);
                if (buf.readableBytes() == 0) {
                    container.writeToNBT(baseline);
                } else {
                    container.readFromNBT((NBTTagCompound) baseline.copy());
                    tile.validate();
                }
            } catch (Throwable ignored) {
                container.readFromNBT((NBTTagCompound) baseline.copy());
                tile.validate();
            }
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void appendCableBusStatStacks(@Nullable TileEntity tileEntity, List<ItemStack> output) {
        if (!(tileEntity instanceof TileCableBus cableBusTile) || output == null) {
            return;
        }
        for (ForgeDirection direction : ForgeDirection.values()) {
            appendPartStatStack(cableBusTile.getPart(direction), output);
            if (direction != ForgeDirection.UNKNOWN) {
                appendFacadeStatStack(cableBusTile, direction, output);
            }
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void appendCableBusStatEntries(@Nullable TileEntity tileEntity,
        List<GuideBlockStatsStackResolver.ResolvedStack> output, int x, int y, int z) {
        if (!(tileEntity instanceof TileCableBus cableBusTile) || output == null) {
            return;
        }
        for (ForgeDirection direction : ForgeDirection.values()) {
            appendPartStatEntry(cableBusTile.getPart(direction), output, x, y, z, direction);
            if (direction != ForgeDirection.UNKNOWN) {
                appendFacadeStatEntry(cableBusTile, direction, output, x, y, z);
            }
        }
    }

    private static void appendPartStatStack(@Nullable IPart part, List<ItemStack> output) {
        if (part == null) {
            return;
        }
        ItemStack stack = safePartStack(part, PartItemStack.Break);
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.World);
        }
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.Pick);
        }
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.Network);
        }
        appendCopy(output, stack);
    }

    private static void appendPartStatEntry(@Nullable IPart part,
        List<GuideBlockStatsStackResolver.ResolvedStack> output, int x, int y, int z, ForgeDirection direction) {
        if (part == null) {
            return;
        }
        ItemStack stack = safePartStack(part, PartItemStack.Break);
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.World);
        }
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.Pick);
        }
        if (stack == null) {
            stack = safePartStack(part, PartItemStack.Network);
        }
        appendEntryCopy(output, stack, approximateCableBusBounds(x, y, z, direction, false));
    }

    @Nullable
    private static ItemStack safePartStack(IPart part, PartItemStack type) {
        try {
            ItemStack stack = part.getItemStack(type);
            return stack != null ? stack.copy() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void appendFacadeStatStack(TileCableBus cableBusTile, ForgeDirection direction,
        List<ItemStack> output) {
        try {
            IFacadePart facade = cableBusTile.getFacadeContainer()
                .getFacade(direction);
            if (facade != null) {
                appendCopy(output, facade.getItemStack());
            }
        } catch (Throwable ignored) {}
    }

    private static void appendFacadeStatEntry(TileCableBus cableBusTile, ForgeDirection direction,
        List<GuideBlockStatsStackResolver.ResolvedStack> output, int x, int y, int z) {
        try {
            IFacadePart facade = cableBusTile.getFacadeContainer()
                .getFacade(direction);
            if (facade != null) {
                appendEntryCopy(output, facade.getItemStack(), approximateCableBusBounds(x, y, z, direction, true));
            }
        } catch (Throwable ignored) {}
    }

    private static void appendCopy(List<ItemStack> output, @Nullable ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        output.add(stack.copy());
    }

    private static void appendEntryCopy(List<GuideBlockStatsStackResolver.ResolvedStack> output,
        @Nullable ItemStack stack, AxisAlignedBB bounds) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        output.add(new GuideBlockStatsStackResolver.ResolvedStack(stack.copy(), bounds));
    }

    private static AxisAlignedBB approximateCableBusBounds(int x, int y, int z, ForgeDirection direction,
        boolean facade) {
        double min = facade ? 0.0D : 0.25D;
        double max = facade ? 1.0D : 0.75D;
        double sideMin = facade ? 0.0D : 0.375D;
        double sideMax = facade ? 1.0D : 0.625D;
        double thickness = facade ? 0.125D : 0.25D;
        return switch (direction) {
            case DOWN -> AxisAlignedBB
                .getBoundingBox(x + sideMin, y, z + sideMin, x + sideMax, y + thickness, z + sideMax);
            case UP -> AxisAlignedBB
                .getBoundingBox(x + sideMin, y + 1.0D - thickness, z + sideMin, x + sideMax, y + 1.0D, z + sideMax);
            case NORTH -> AxisAlignedBB
                .getBoundingBox(x + sideMin, y + sideMin, z, x + sideMax, y + sideMax, z + thickness);
            case SOUTH -> AxisAlignedBB
                .getBoundingBox(x + sideMin, y + sideMin, z + 1.0D - thickness, x + sideMax, y + sideMax, z + 1.0D);
            case WEST -> AxisAlignedBB
                .getBoundingBox(x, y + sideMin, z + sideMin, x + thickness, y + sideMax, z + sideMax);
            case EAST -> AxisAlignedBB
                .getBoundingBox(x + 1.0D - thickness, y + sideMin, z + sideMin, x + 1.0D, y + sideMax, z + sideMax);
            default -> AxisAlignedBB.getBoundingBox(x + min, y + min, z + min, x + max, y + max, z + max);
        };
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static int computeCableConnectionMask(TileCableBus cableBusTile, GuidebookLevel level) {
        return computeCableConnectionMask(cableBusTile.getCableBus(), level);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static int computeCableConnectionMask(CableBusContainer container, GuidebookLevel level) {
        TileEntity tile = container.getTile();
        int x = tile.xCoord;
        int y = tile.yCoord;
        int z = tile.zCoord;

        int cs = 0;
        for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
            TileEntity adj = level.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ);
            CableBusContainer adjContainer = resolveCableContainer(adj);
            boolean sourceHasSidePart = container.getPart(dir) != null;
            boolean sourceBlocked = isBlocked(container, dir);
            boolean sourceCanConnect = container.getCableConnectionType(dir) != AECableType.NONE;
            boolean neighborCanConnect;
            boolean neighborFaceBlockedByPart = false;
            boolean neighborBlocked = false;
            boolean neighborAcceptsSide = true;

            if (adjContainer != null) {
                ForgeDirection opposite = dir.getOpposite();
                neighborCanConnect = canConnectCableBusOnSide(adjContainer, opposite);
                neighborFaceBlockedByPart = Ae2CableConnectionRules
                    .facePartBlocksAdjacentCable(adjContainer.getPart(opposite) != null, neighborCanConnect);
                neighborBlocked = isBlocked(adjContainer, opposite);
            } else if (adj instanceof IGridHost adjHost) {
                ForgeDirection opposite = dir.getOpposite();
                neighborCanConnect = adjHost.getCableConnectionType(opposite) != AECableType.NONE;
                if (adjHost instanceof IGridProxyable adjProxyable) {
                    AENetworkProxy proxy = null;
                    try {
                        proxy = adjProxyable.getProxy();
                    } catch (Throwable ignored) {}
                    if (proxy == null) {
                        continue;
                    }
                    neighborAcceptsSide = proxy.getConnectableSides()
                        .contains(opposite);
                }
            } else {
                continue;
            }

            if (!Ae2CableConnectionRules.shouldConnect(
                sourceHasSidePart,
                sourceBlocked,
                sourceCanConnect,
                neighborCanConnect,
                neighborFaceBlockedByPart,
                neighborBlocked,
                neighborAcceptsSide)) {
                continue;
            }
            cs |= (1 << dir.ordinal());
        }
        return cs;
    }

    @Optional.Method(modid = "appliedenergistics2")
    @Nullable
    private static CableBusContainer resolveCableContainer(@Nullable TileEntity tileEntity) {
        return Ae2CableStructureSupport.resolveCableContainer(tileEntity);
    }

    private static boolean isBlocked(CableBusContainer container, ForgeDirection direction) {
        try {
            return container.isBlocked(direction);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static boolean canConnectCableBusOnSide(CableBusContainer container, ForgeDirection direction) {
        try {
            return container.getCableConnectionType(direction) != AECableType.NONE;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void applyNonCableBaseTilePreview(AEBaseTile aeTile, GuidebookLevel level) {
        if (aeTile instanceof TileCableBus) {
            return;
        }
        long posKey = GuidebookLevel.packPos(aeTile.xCoord, aeTile.yCoord, aeTile.zCoord);
        byte[] blob = level.previewAuthorityStore()
            .get(posKey, Ae2BaseTileNetworkStreamPreview.SUPPLEMENT_ID);
        boolean applied = blob != null && blob.length > 0
            && Ae2BaseTileNetworkStreamPreview.applyAuthorityToPreviewTile(aeTile, blob);
        if (applied) {
            syncSpecialPreviewConnectableSides(aeTile);
            return;
        }
        syncDescriptionPacket(aeTile);
        syncSpecialPreviewConnectableSides(aeTile);
    }

    @Optional.Method(modid = "appliedenergistics2")
    public static void syncDescriptionPacket(AEBaseTile tile) {
        try {
            Packet packet = tile.getDescriptionPacket();
            if (packet instanceof S35PacketUpdateTileEntity updatePacket) {
                tile.onDataPacket(null, updatePacket);
            }
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static void syncSpecialPreviewConnectableSides(AEBaseTile aeTile) {
        if (aeTile instanceof TileSpatialPylon spatialPylon) {
            syncSpatialPylonValidSides(spatialPylon);
        }
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static void syncSpatialPylonValidSides(TileSpatialPylon spatialPylon) {
        EnumSet<ForgeDirection> validSides = hasSpatialPylonClusterState(spatialPylon)
            ? EnumSet.allOf(ForgeDirection.class)
            : EnumSet.noneOf(ForgeDirection.class);
        try {
            spatialPylon.getProxy()
                .setValidSides(validSides);
        } catch (Throwable ignored) {}
    }

    @Optional.Method(modid = "appliedenergistics2")
    private static boolean hasSpatialPylonClusterState(TileSpatialPylon spatialPylon) {
        return (spatialPylon.getDisplayBits() & TileSpatialPylon.MB_STATUS) != 0;
    }
}
