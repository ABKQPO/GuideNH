package com.hfstudio.guidenh.guide.scene.structurelib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.stats.StatBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.gtnewhorizon.structurelib.StructureEvent.StructureElementVisitedEvent;
import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;

final class StructureLibRuntimeFacade implements StructureLibFacade {

    private static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    private static final int CONTROLLER_X = 0;
    private static final int CONTROLLER_Y = 64;
    private static final int CONTROLLER_Z = 0;
    private static final int MIN_CHANNEL = 1;
    private static final int MAX_CHANNEL = 50;
    private static final StructureLibPreviewMetadataFactory PREVIEW_METADATA_FACTORY = new StructureLibPreviewMetadataFactory(
        new StructureLibElementTooltipResolver());

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public StructureLibImportResult importScene(StructureLibImportRequest request) {
        List<String> warnings = new ArrayList<>();
        ResolvedController controller;
        try {
            controller = resolveController(request);
        } catch (IllegalArgumentException e) {
            return StructureLibImportResult.failure(e.getMessage(), warnings, null);
        }

        if (request.getPiece() != null) {
            warnings.add(
                "StructureLib runtime preview currently uses the controller's default constructable and ignores piece selection.");
        }

        int maxChannel = estimateMaxChannel(request, controller);
        int effectiveChannel = clampChannel(resolveRequestedChannel(request), MIN_CHANNEL, maxChannel);
        Integer requestedChannel = request.getChannel();
        if (requestedChannel != null && requestedChannel.intValue() != effectiveChannel) {
            warnings.add(
                "Requested StructureLib channel " + requestedChannel
                    + " was clamped to "
                    + effectiveChannel
                    + " for preview generation.");
        }

        BuildSnapshot snapshot = buildSnapshot(request, controller, effectiveChannel, warnings);
        if (!snapshot.success) {
            return StructureLibImportResult.failure(snapshot.errorMessage, warnings, null);
        }

        StructureLibSceneMetadata metadata = PREVIEW_METADATA_FACTORY.createMetadata(
            request,
            MIN_CHANNEL,
            maxChannel,
            effectiveChannel,
            snapshot.absoluteBlocks,
            snapshot.visitedElements,
            snapshot.triggerStack,
            snapshot.world,
            snapshot.constructable,
            snapshot.actor);

        return StructureLibImportResult.success(snapshot.blocks, warnings, metadata);
    }

    private static int estimateMaxChannel(StructureLibImportRequest request, ResolvedController controller) {
        BuildSnapshot previous = buildSnapshot(request, controller, MIN_CHANNEL, new ArrayList<String>());
        if (!previous.success) {
            return MIN_CHANNEL;
        }
        String previousFingerprint = previous.fingerprint;
        for (int channel = MIN_CHANNEL + 1; channel <= MAX_CHANNEL; channel++) {
            BuildSnapshot current = buildSnapshot(request, controller, channel, new ArrayList<String>());
            if (!current.success) {
                return Math.max(MIN_CHANNEL, channel - 1);
            }
            if (previousFingerprint.equals(current.fingerprint)) {
                return Math.max(MIN_CHANNEL, channel - 1);
            }
            previousFingerprint = current.fingerprint;
        }
        return MAX_CHANNEL;
    }

    private static BuildSnapshot buildSnapshot(StructureLibImportRequest request, ResolvedController controller,
        int channel, List<String> warnings) {
        GuidebookLevel level = new GuidebookLevel();
        World world;
        try {
            world = level.getOrCreateFakeWorld();
        } catch (Throwable t) {
            LOG.warn("Failed to create Guidebook fake world for StructureLib preview", t);
            return BuildSnapshot.failure("StructureLib preview requires an active client world.");
        }

        PreviewFakePlayer fakePlayer = new PreviewFakePlayer(world);
        TileEntity controllerTile = placeController(level, world, fakePlayer, controller, warnings);
        if (controllerTile == null) {
            return BuildSnapshot.failure(
                "Failed to create a controller tile for " + request.getController() + " in the preview world.");
        }

        applyRequestedAlignment(controllerTile, request, warnings);
        IConstructable constructable = resolveConstructable(controllerTile);
        if (constructable == null) {
            return BuildSnapshot.failure(
                "Failed to resolve a StructureLib constructable for controller " + request.getController() + ".");
        }

        ItemStack triggerStack = createTriggerStack(channel);
        List<StructureLibPreviewMetadataFactory.VisitedStructureElement> visitedElements = Collections.emptyList();
        Object instrumentId = new Object();
        StructureVisitCollector visitCollector = new StructureVisitCollector(instrumentId, world);
        boolean instrumentEnabled = false;
        try {
            StructureLibAPI.enableInstrument(instrumentId);
            instrumentEnabled = true;
            MinecraftForge.EVENT_BUS.register(visitCollector);
        } catch (IllegalStateException ignored) {
            warnings
                .add("StructureLib instrumentation was already active; preview tooltip metadata may be incomplete.");
        } catch (Throwable t) {
            warnings.add("StructureLib instrumentation setup failed; preview tooltip metadata may be incomplete.");
            LOG.warn("Failed to enable StructureLib instrumentation for controller {}", request.getController(), t);
        }

        try {
            constructable.construct(triggerStack.copy(), false);
        } catch (Throwable t) {
            LOG.warn("StructureLib construct() failed for controller {}", request.getController(), t);
            return BuildSnapshot.failure("StructureLib construct() failed: " + sanitizeMessage(t.getMessage()));
        } finally {
            if (instrumentEnabled) {
                visitedElements = visitCollector.snapshot();
                MinecraftForge.EVENT_BUS.unregister(visitCollector);
                try {
                    StructureLibAPI.disableInstrument();
                } catch (IllegalStateException ignored) {}
            }
        }

        SnapshotBlocksResult snapshotBlocks = snapshotBlocks(level);
        if (snapshotBlocks.blocks.isEmpty()) {
            return BuildSnapshot.failure("StructureLib preview did not place any blocks.");
        }
        return BuildSnapshot.success(
            snapshotBlocks.blocks,
            snapshotBlocks.absoluteBlocks,
            visitedElements,
            buildFingerprint(snapshotBlocks.blocks),
            world,
            triggerStack,
            constructable,
            fakePlayer);
    }

    private static TileEntity placeController(GuidebookLevel level, World world, PreviewFakePlayer fakePlayer,
        ResolvedController controller, List<String> warnings) {
        Item item = Item.getItemFromBlock(controller.block);
        if (item != null) {
            try {
                ItemStack stack = new ItemStack(item, 1, controller.meta);
                fakePlayer.inventory.mainInventory[fakePlayer.inventory.currentItem] = stack;
                item.onItemUse(
                    stack,
                    fakePlayer,
                    world,
                    CONTROLLER_X,
                    CONTROLLER_Y,
                    CONTROLLER_Z,
                    0,
                    CONTROLLER_X,
                    CONTROLLER_Y - 1,
                    CONTROLLER_Z);
            } catch (Throwable t) {
                warnings.add(
                    "Controller item placement failed in StructureLib preview, falling back to direct block placement.");
                LOG.warn("StructureLib controller item placement failed for {}", controller.blockId, t);
            }
        }

        TileEntity tile = world.getTileEntity(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
        if (tile != null) {
            level.setExplicitBlockId(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z, controller.blockId);
            return tile;
        }

        TileEntity fallbackTile = null;
        try {
            if (controller.block.hasTileEntity(controller.meta)) {
                fallbackTile = controller.block.createTileEntity(world, controller.meta);
            }
        } catch (Throwable t) {
            LOG.warn("Direct controller tile creation failed for {}", controller.blockId, t);
        }

        level.setBlock(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z, controller.block, controller.meta, fallbackTile);
        level.setExplicitBlockId(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z, controller.blockId);
        return world.getTileEntity(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
    }

    private static void applyRequestedAlignment(TileEntity controllerTile, StructureLibImportRequest request,
        List<String> warnings) {
        if (!(controllerTile instanceof IAlignment alignment)) {
            if (request.getFacing() != null || request.getRotation() != null || request.getFlip() != null) {
                warnings.add(
                    "Controller does not expose StructureLib alignment controls; preview used the default facing.");
            }
            return;
        }

        ForgeDirection direction = parseDirection(request.getFacing(), warnings);
        Rotation rotation = parseRotation(request.getRotation(), warnings);
        Flip flip = parseFlip(request.getFlip(), warnings);
        ExtendedFacing requestedFacing = ExtendedFacing.of(direction, rotation, flip);
        if (!alignment.checkedSetExtendedFacing(requestedFacing)) {
            warnings.add(
                "Requested StructureLib facing/rotation/flip is not valid for this controller; preview used the default alignment.");
        }
    }

    @Nullable
    private static IConstructable resolveConstructable(TileEntity controllerTile) {
        if (controllerTile instanceof IConstructableProvider provider) {
            IConstructable constructable = provider.getConstructable();
            if (constructable != null) {
                return constructable;
            }
        }
        if (controllerTile instanceof IConstructable constructable) {
            return constructable;
        }
        if (IMultiblockInfoContainer.contains(controllerTile.getClass())) {
            IMultiblockInfoContainer<TileEntity> container = IMultiblockInfoContainer.get(controllerTile.getClass());
            if (container != null) {
                ExtendedFacing facing = controllerTile instanceof IAlignment alignment ? alignment.getExtendedFacing()
                    : ExtendedFacing.DEFAULT;
                return container.toConstructable(controllerTile, facing);
            }
        }
        return null;
    }

    private static ItemStack createTriggerStack(int channel) {
        return new ItemStack(StructureLibAPI.getDefaultHologramItem(), Math.max(MIN_CHANNEL, channel));
    }

    private static SnapshotBlocksResult snapshotBlocks(GuidebookLevel level) {
        List<AbsolutePlacedBlock> absoluteBlocks = new ArrayList<>();
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (int[] filledBlock : level.getFilledBlocks()) {
            int x = filledBlock[0];
            int y = filledBlock[1];
            int z = filledBlock[2];
            Block block = level.getBlock(x, y, z);
            if (block == null || block == Blocks.air) {
                continue;
            }
            int meta = level.getBlockMetadata(x, y, z);
            TileEntity tile = level.getTileEntity(x, y, z);
            absoluteBlocks
                .add(new AbsolutePlacedBlock(x, y, z, block, meta, serializeTile(tile), resolveBlockId(block)));
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
        }

        if (absoluteBlocks.isEmpty()) {
            return SnapshotBlocksResult.empty();
        }

        List<StructureLibImportResult.PlacedBlock> normalizedBlocks = new ArrayList<>(absoluteBlocks.size());
        List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> previewBlocks = new ArrayList<>(
            absoluteBlocks.size());
        for (AbsolutePlacedBlock block : absoluteBlocks) {
            normalizedBlocks.add(
                new StructureLibImportResult.PlacedBlock(
                    block.x - minX,
                    block.y - minY,
                    block.z - minZ,
                    block.block,
                    block.meta,
                    block.tileTag,
                    block.blockId));
            previewBlocks.add(new StructureLibPreviewMetadataFactory.AbsolutePreviewBlock(block.x, block.y, block.z));
        }
        normalizedBlocks.sort(
            Comparator.comparingInt(StructureLibImportResult.PlacedBlock::getX)
                .thenComparingInt(StructureLibImportResult.PlacedBlock::getY)
                .thenComparingInt(StructureLibImportResult.PlacedBlock::getZ));
        return new SnapshotBlocksResult(normalizedBlocks, previewBlocks);
    }

    private static String buildFingerprint(List<StructureLibImportResult.PlacedBlock> blocks) {
        StringBuilder builder = new StringBuilder(blocks.size() * 24);
        for (StructureLibImportResult.PlacedBlock block : blocks) {
            builder.append(block.getX())
                .append(',')
                .append(block.getY())
                .append(',')
                .append(block.getZ())
                .append(':')
                .append(block.getBlockId())
                .append('@')
                .append(block.getMeta())
                .append(';');
        }
        return builder.toString();
    }

    @Nullable
    private static NBTTagCompound serializeTile(@Nullable TileEntity tile) {
        if (tile == null) {
            return null;
        }
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag);
            return tag;
        } catch (Throwable t) {
            LOG.warn(
                "Failed to serialize preview tile entity {}",
                tile.getClass()
                    .getName(),
                t);
            return null;
        }
    }

    @Nullable
    private static String resolveBlockId(@Nullable Block block) {
        if (block == null) {
            return null;
        }

        try {
            GameRegistry.UniqueIdentifier uniqueIdentifier = GameRegistry.findUniqueIdentifierFor(block);
            if (uniqueIdentifier != null) {
                return uniqueIdentifier.toString();
            }
        } catch (RuntimeException ignored) {}

        Object registryName = Block.blockRegistry.getNameForObject(block);
        if (registryName != null) {
            String normalized = normalizeBlockId(registryName.toString());
            if (normalized != null) {
                return normalized;
            }
        }

        return normalizeBlockId(block.getUnlocalizedName());
    }

    private static String sanitizeMessage(@Nullable String message) {
        if (message == null) {
            return "unknown error";
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? "unknown error" : trimmed;
    }

    private static ResolvedController resolveController(StructureLibImportRequest request) {
        GuideBlockMatcher matcher = GuideBlockMatcher.parse(request.getController());
        Block block = (Block) Block.blockRegistry.getObject(matcher.getBlockId());
        if (block == null || block == Blocks.air) {
            throw new IllegalArgumentException(
                "Could not resolve StructureLib controller block: " + request.getController());
        }
        return new ResolvedController(
            matcher.getBlockId(),
            block,
            matcher.getMeta() != null ? matcher.getMeta()
                .intValue() : 0);
    }

    private static int resolveRequestedChannel(StructureLibImportRequest request) {
        Integer channel = request.getChannel();
        return channel != null ? Math.max(MIN_CHANNEL, channel.intValue()) : MIN_CHANNEL;
    }

    private static int clampChannel(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        return value > maxValue ? maxValue : value;
    }

    private static ForgeDirection parseDirection(@Nullable String rawFacing, List<String> warnings) {
        if (rawFacing == null || rawFacing.trim()
            .isEmpty()) {
            return ForgeDirection.NORTH;
        }
        String normalized = rawFacing.trim()
            .toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "down" -> ForgeDirection.DOWN;
            case "up" -> ForgeDirection.UP;
            case "north" -> ForgeDirection.NORTH;
            case "south" -> ForgeDirection.SOUTH;
            case "west" -> ForgeDirection.WEST;
            case "east" -> ForgeDirection.EAST;
            default -> {
                warnings.add("Unsupported StructureLib facing '" + rawFacing + "'; preview used north.");
                yield ForgeDirection.NORTH;
            }
        };
    }

    private static Rotation parseRotation(@Nullable String rawRotation, List<String> warnings) {
        if (rawRotation == null || rawRotation.trim()
            .isEmpty()) {
            return Rotation.NORMAL;
        }
        Rotation rotation = Rotation.byName(normalizeRotation(rawRotation));
        if (rotation != null) {
            return rotation;
        }
        warnings.add("Unsupported StructureLib rotation '" + rawRotation + "'; preview used normal rotation.");
        return Rotation.NORMAL;
    }

    private static Flip parseFlip(@Nullable String rawFlip, List<String> warnings) {
        if (rawFlip == null || rawFlip.trim()
            .isEmpty()) {
            return Flip.NONE;
        }
        Flip flip = Flip.byName(normalizeFlip(rawFlip));
        if (flip != null) {
            return flip;
        }
        warnings.add("Unsupported StructureLib flip '" + rawFlip + "'; preview used no flip.");
        return Flip.NONE;
    }

    private static String normalizeRotation(String rawRotation) {
        String normalized = rawRotation.trim()
            .toLowerCase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ');
        return switch (normalized) {
            case "90", "clockwise 90" -> "clockwise";
            case "180", "upside down 180" -> "upside down";
            case "270", "counter clockwise 90", "counterclockwise 90" -> "counter clockwise";
            default -> normalized;
        };
    }

    private static String normalizeFlip(String rawFlip) {
        String normalized = rawFlip.trim()
            .toLowerCase(Locale.ROOT)
            .replace('_', ' ')
            .replace('-', ' ');
        return switch (normalized) {
            case "mirror left right", "left right", "x" -> "horizontal";
            case "mirror front back", "front back", "z", "y" -> "vertical";
            default -> normalized;
        };
    }

    @Nullable
    private static String normalizeBlockId(@Nullable String blockId) {
        if (blockId == null) {
            return null;
        }
        String trimmed = blockId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.startsWith("tile.") && trimmed.length() > 5) {
            return "minecraft:" + trimmed.substring(5);
        }
        int tileNamespaceIndex = trimmed.indexOf(":tile.");
        if (tileNamespaceIndex >= 0) {
            return trimmed.substring(0, tileNamespaceIndex + 1) + trimmed.substring(tileNamespaceIndex + 6);
        }
        return trimmed.indexOf(':') >= 0 ? trimmed : "minecraft:" + trimmed;
    }

    private static final class ResolvedController {

        private final String blockId;
        private final Block block;
        private final int meta;

        private ResolvedController(String blockId, Block block, int meta) {
            this.blockId = blockId;
            this.block = block;
            this.meta = meta;
        }
    }

    private static final class AbsolutePlacedBlock {

        private final int x;
        private final int y;
        private final int z;
        private final Block block;
        private final int meta;
        @Nullable
        private final NBTTagCompound tileTag;
        @Nullable
        private final String blockId;

        private AbsolutePlacedBlock(int x, int y, int z, Block block, int meta, @Nullable NBTTagCompound tileTag,
            @Nullable String blockId) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.block = block;
            this.meta = meta;
            this.tileTag = tileTag != null ? (NBTTagCompound) tileTag.copy() : null;
            this.blockId = blockId;
        }
    }

    private static final class BuildSnapshot {

        private final boolean success;
        private final List<StructureLibImportResult.PlacedBlock> blocks;
        private final List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks;
        private final List<StructureLibPreviewMetadataFactory.VisitedStructureElement> visitedElements;
        private final String fingerprint;
        @Nullable
        private final World world;
        private final ItemStack triggerStack;
        @Nullable
        private final Object constructable;
        @Nullable
        private final EntityPlayer actor;
        @Nullable
        private final String errorMessage;

        private BuildSnapshot(boolean success, List<StructureLibImportResult.PlacedBlock> blocks,
            List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks,
            List<StructureLibPreviewMetadataFactory.VisitedStructureElement> visitedElements, String fingerprint,
            @Nullable World world, ItemStack triggerStack, @Nullable Object constructable, @Nullable EntityPlayer actor,
            @Nullable String errorMessage) {
            this.success = success;
            this.blocks = blocks;
            this.absoluteBlocks = absoluteBlocks;
            this.visitedElements = visitedElements;
            this.fingerprint = fingerprint;
            this.world = world;
            this.triggerStack = triggerStack;
            this.constructable = constructable;
            this.actor = actor;
            this.errorMessage = errorMessage;
        }

        private static BuildSnapshot success(List<StructureLibImportResult.PlacedBlock> blocks,
            List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks,
            List<StructureLibPreviewMetadataFactory.VisitedStructureElement> visitedElements, String fingerprint,
            World world, ItemStack triggerStack, Object constructable, EntityPlayer actor) {
            return new BuildSnapshot(
                true,
                blocks,
                absoluteBlocks,
                visitedElements,
                fingerprint,
                world,
                triggerStack,
                constructable,
                actor,
                null);
        }

        private static BuildSnapshot failure(String errorMessage) {
            return new BuildSnapshot(
                false,
                Collections.<StructureLibImportResult.PlacedBlock>emptyList(),
                Collections.<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock>emptyList(),
                Collections.<StructureLibPreviewMetadataFactory.VisitedStructureElement>emptyList(),
                "",
                null,
                new ItemStack(StructureLibAPI.getDefaultHologramItem(), MIN_CHANNEL),
                null,
                null,
                errorMessage);
        }
    }

    private static final class SnapshotBlocksResult {

        private static final SnapshotBlocksResult EMPTY = new SnapshotBlocksResult(
            Collections.<StructureLibImportResult.PlacedBlock>emptyList(),
            Collections.<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock>emptyList());

        private final List<StructureLibImportResult.PlacedBlock> blocks;
        private final List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks;

        private SnapshotBlocksResult(List<StructureLibImportResult.PlacedBlock> blocks,
            List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks) {
            this.blocks = blocks;
            this.absoluteBlocks = absoluteBlocks;
        }

        private static SnapshotBlocksResult empty() {
            return EMPTY;
        }
    }

    private static final class StructureVisitCollector {

        private final Object instrumentId;
        private final World world;
        private final List<StructureLibPreviewMetadataFactory.VisitedStructureElement> visitedElements = new ArrayList<>();

        private StructureVisitCollector(Object instrumentId, World world) {
            this.instrumentId = instrumentId;
            this.world = world;
        }

        @SubscribeEvent
        public void onStructureElementVisited(StructureElementVisitedEvent event) {
            if (event == null || event.getElement() == null
                || event.getWorld() != world
                || !instrumentId.equals(event.getInstrumentIdentifier())) {
                return;
            }
            visitedElements.add(
                new StructureLibPreviewMetadataFactory.VisitedStructureElement(
                    event.getX(),
                    event.getY(),
                    event.getZ(),
                    event.getElement()));
        }

        private List<StructureLibPreviewMetadataFactory.VisitedStructureElement> snapshot() {
            if (visitedElements.isEmpty()) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(new ArrayList<>(visitedElements));
        }
    }

    private static final class PreviewFakePlayer extends EntityPlayer {

        private PreviewFakePlayer(World world) {
            super(world, new GameProfile(UUID.fromString("9c7ef542-6ab6-4524-b7d7-8caaf8df467c"), "GuideNHPreview"));
            capabilities.isCreativeMode = true;
            noClip = true;
        }

        @Override
        public void addChatMessage(IChatComponent message) {}

        @Override
        public boolean canCommandSenderUseCommand(int i, String s) {
            return false;
        }

        @Override
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
        }

        @Override
        public void addChatComponentMessage(IChatComponent message) {}

        @Override
        public void addStat(StatBase par1StatBase, int par2) {}

        @Override
        public void openGui(Object mod, int modGuiId, World world, int x, int y, int z) {}

        @Override
        public boolean isEntityInvulnerable() {
            return true;
        }

        @Override
        public boolean canAttackPlayer(EntityPlayer player) {
            return false;
        }

        @Override
        public void onDeath(DamageSource source) {}

        @Override
        public void onUpdate() {}

        @Override
        public void travelToDimension(int dim) {}
    }
}
