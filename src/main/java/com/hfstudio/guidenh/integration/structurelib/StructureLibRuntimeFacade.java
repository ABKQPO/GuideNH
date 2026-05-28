package com.hfstudio.guidenh.integration.structurelib;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
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
import org.jetbrains.annotations.Nullable;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructable;
import com.gtnewhorizon.structurelib.alignment.constructable.IConstructableProvider;
import com.gtnewhorizon.structurelib.alignment.constructable.IMultiblockInfoContainer;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.gtnewhorizon.structurelib.alignment.enumerable.Flip;
import com.gtnewhorizon.structurelib.alignment.enumerable.Rotation;
import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.hfstudio.guidenh.guide.scene.level.GuidebookLevel;
import com.hfstudio.guidenh.guide.scene.support.GuideBlockMatcher;
import com.hfstudio.guidenh.guide.scene.support.GuideDebugLog;
import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.registry.GameRegistry;

public class StructureLibRuntimeFacade implements StructureLibFacade {

    public static final Logger LOG = LogManager.getLogger("GuideNH/ScenePreview");
    public static final int CONTROLLER_X = 0;
    public static final int CONTROLLER_Y = 64;
    public static final int CONTROLLER_Z = 0;
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 50;
    public static final int SURVIVAL_BUILD_BUDGET = 512;
    public static final int SURVIVAL_BUILD_MAX_ROUNDS = 256;
    public static final int MAX_CONTROL_ANALYSIS_CACHE_ENTRIES = 128;
    public static final int MAX_ANALYSIS_SNAPSHOT_CACHE_ENTRIES = 512;
    public static final int MAX_IMPORT_RESULT_CACHE_ENTRIES = 64;
    public static final int MAX_STABLE_ANALYSIS_FINGERPRINT_RUN = 2;
    public static final StructureLibPreviewMetadataFactory PREVIEW_METADATA_FACTORY = new StructureLibPreviewMetadataFactory(
        new StructureLibElementTooltipResolver());
    public static final StructureLibBoundedCache<AnalysisKey, ControlAnalysis> CONTROL_ANALYSIS_CACHE = new StructureLibBoundedCache<>(
        MAX_CONTROL_ANALYSIS_CACHE_ENTRIES);
    public static final StructureLibBoundedCache<AnalysisSnapshotKey, AnalysisSnapshot> ANALYSIS_SNAPSHOT_CACHE = new StructureLibBoundedCache<>(
        MAX_ANALYSIS_SNAPSHOT_CACHE_ENTRIES);
    public static final StructureLibBoundedCache<StructureLibImportCacheKey, StructureLibImportResult> IMPORT_RESULT_CACHE = new StructureLibBoundedCache<>(
        MAX_IMPORT_RESULT_CACHE_ENTRIES);

    public StructureLibRuntimeFacade() {}

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public StructureLibImportResult importScene(StructureLibImportRequest request) {
        try {
            BuildContext context = new BuildContext();
            try {
                return importScene(request, context);
            } finally {
                context.clear();
            }
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "Failed to create Guidebook fake world for StructureLib preview", t);
            return StructureLibImportResult.failure("StructureLib preview requires an active client world.");
        }
    }

    public StructureLibImportResult importScene(StructureLibImportRequest request, BuildContext context) {
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

        try {
            ControlAnalysis controlAnalysis = analyzeControls(request, controller, context);
            StructureLibPreviewSelection requestedSelection = request.getPreviewSelection();
            StructureLibPreviewSelection effectiveSelection = controlAnalysis.clampSelection(requestedSelection);
            Integer requestedChannel = request.getChannel();
            if (requestedChannel != null && requestedChannel != effectiveSelection.getMasterTier()) {
                warnings.add(
                    "Requested StructureLib channel " + requestedChannel
                        + " was clamped to "
                        + effectiveSelection.getMasterTier()
                        + " for preview generation.");
            }

            StructureLibImportCacheKey importCacheKey = new StructureLibImportCacheKey(
                request.getController(),
                request.getPiece(),
                request.getFacing(),
                request.getRotation(),
                request.getFlip(),
                requestedChannel,
                effectiveSelection);
            StructureLibImportResult cached = IMPORT_RESULT_CACHE.get(importCacheKey);
            if (cached != null) {
                return cached.withWarnings(mergeWarnings(warnings, cached.getWarnings()));
            }

            BuildSnapshot snapshot = buildSnapshot(request, controller, effectiveSelection, warnings, context);
            if (!snapshot.success) {
                return StructureLibImportResult.failure(snapshot.errorMessage, warnings, null);
            }

            StructureLibSceneMetadata metadata = PREVIEW_METADATA_FACTORY.createMetadata(
                request,
                effectiveSelection,
                Math.max(controlAnalysis.maxTotalTier, effectiveSelection.getMasterTier()),
                mergeChannelMaxTierMap(controlAnalysis.channelMaxTierMap, effectiveSelection),
                snapshot.absoluteBlocks,
                snapshot.visitedElementsByPos,
                snapshot.triggerStack,
                snapshot.world,
                snapshot.fingerprint,
                snapshot.constructable,
                snapshot.actor);

            StructureLibImportResult result = StructureLibImportResult.success(snapshot.blocks, warnings, metadata);
            IMPORT_RESULT_CACHE.put(importCacheKey, result);
            return result;
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "StructureLib import failed for controller {}", request.getController(), t);
            return StructureLibImportResult
                .failure("StructureLib import failed: " + sanitizeMessage(t.getMessage()), warnings, null);
        } finally {
            context.clear();
        }
    }

    public static ControlAnalysis analyzeControls(StructureLibImportRequest request, ResolvedController controller) {
        try {
            BuildContext context = new BuildContext();
            try {
                return analyzeControls(request, controller, context);
            } finally {
                context.clear();
            }
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "Failed to create Guidebook fake world for StructureLib control analysis", t);
            return new ControlAnalysis(MIN_TIER, Map.of());
        }
    }

    public static ControlAnalysis analyzeControls(StructureLibImportRequest request, ResolvedController controller,
        BuildContext context) {
        AnalysisKey key = new AnalysisKey(
            request.getController(),
            request.getPiece(),
            request.getFacing(),
            request.getRotation(),
            request.getFlip());
        ControlAnalysis cached = CONTROL_ANALYSIS_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        LinkedHashSet<String> discoveredChannels = new LinkedHashSet<>();
        int maxTotalTier = estimateMaxTotalTier(request, controller, discoveredChannels, context);
        LinkedHashMap<String, Integer> channelMaxTierMap = estimateChannelMaxTiers(
            request,
            controller,
            discoveredChannels,
            context);
        ControlAnalysis created = new ControlAnalysis(maxTotalTier, channelMaxTierMap);
        CONTROL_ANALYSIS_CACHE.put(key, created);
        return created;
    }

    public static int estimateMaxTotalTier(StructureLibImportRequest request, ResolvedController controller,
        Set<String> discoveredChannels) {
        try {
            BuildContext context = new BuildContext();
            try {
                return estimateMaxTotalTier(request, controller, discoveredChannels, context);
            } finally {
                context.clear();
            }
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "Failed to create Guidebook fake world for StructureLib tier analysis", t);
            return MIN_TIER;
        }
    }

    public static int estimateMaxTotalTier(StructureLibImportRequest request, ResolvedController controller,
        Set<String> discoveredChannels, BuildContext context) {
        BuildSnapshot previous = getOrCreateAnalysisSnapshot(
            request,
            controller,
            StructureLibPreviewSelection.ofMasterTier(MIN_TIER),
            context);
        if (!previous.success) {
            return MIN_TIER;
        }
        collectChannelIds(previous, discoveredChannels);
        String previousFingerprint = previous.fingerprint;
        int stableFingerprintRun = 0;
        for (int tier = MIN_TIER + 1; tier <= MAX_TIER; tier++) {
            BuildSnapshot current = getOrCreateAnalysisSnapshot(
                request,
                controller,
                StructureLibPreviewSelection.ofMasterTier(tier),
                context);
            if (!current.success) {
                return Math.max(MIN_TIER, tier - 1);
            }
            collectChannelIds(current, discoveredChannels);
            if (previousFingerprint.equals(current.fingerprint)) {
                stableFingerprintRun++;
                if (stableFingerprintRun >= MAX_STABLE_ANALYSIS_FINGERPRINT_RUN) {
                    return Math.max(MIN_TIER, tier - stableFingerprintRun);
                }
                continue;
            }
            previousFingerprint = current.fingerprint;
            stableFingerprintRun = 0;
        }
        return MAX_TIER;
    }

    public static LinkedHashMap<String, Integer> estimateChannelMaxTiers(StructureLibImportRequest request,
        ResolvedController controller, Set<String> discoveredChannels) {
        try {
            BuildContext context = new BuildContext();
            try {
                return estimateChannelMaxTiers(request, controller, discoveredChannels, context);
            } finally {
                context.clear();
            }
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "Failed to create Guidebook fake world for StructureLib channel analysis", t);
            return new LinkedHashMap<>();
        }
    }

    public static LinkedHashMap<String, Integer> estimateChannelMaxTiers(StructureLibImportRequest request,
        ResolvedController controller, Set<String> discoveredChannels, BuildContext context) {
        LinkedHashMap<String, Integer> resolved = new LinkedHashMap<>();
        List<String> channelsToProcess = new ArrayList<>(discoveredChannels);
        for (int index = 0; index < channelsToProcess.size(); index++) {
            String channelId = StructureLibPreviewSelection.normalizeChannelId(channelsToProcess.get(index));
            if (channelId == null || resolved.containsKey(channelId)) {
                continue;
            }

            StructureLibPreviewSelection baseSelection = StructureLibPreviewSelection.ofMasterTier(MIN_TIER)
                .withChannelOverride(channelId, MIN_TIER);
            BuildSnapshot previous = getOrCreateAnalysisSnapshot(request, controller, baseSelection, context);
            if (!previous.success) {
                continue;
            }

            collectChannelIds(previous, discoveredChannels);
            if (discoveredChannels.size() > channelsToProcess.size()) {
                channelsToProcess = new ArrayList<>(discoveredChannels);
            }

            int maxTier = MIN_TIER;
            String previousFingerprint = previous.fingerprint;
            int stableFingerprintRun = 0;
            for (int tier = MIN_TIER + 1; tier <= MAX_TIER; tier++) {
                StructureLibPreviewSelection selection = StructureLibPreviewSelection.ofMasterTier(MIN_TIER)
                    .withChannelOverride(channelId, tier);
                BuildSnapshot current = getOrCreateAnalysisSnapshot(request, controller, selection, context);
                if (!current.success) {
                    break;
                }
                collectChannelIds(current, discoveredChannels);
                if (discoveredChannels.size() > channelsToProcess.size()) {
                    channelsToProcess = new ArrayList<>(discoveredChannels);
                }
                if (previousFingerprint.equals(current.fingerprint)) {
                    stableFingerprintRun++;
                    if (stableFingerprintRun >= MAX_STABLE_ANALYSIS_FINGERPRINT_RUN) {
                        break;
                    }
                    continue;
                }
                previousFingerprint = current.fingerprint;
                stableFingerprintRun = 0;
                maxTier = tier;
            }

            if (maxTier > 0) {
                resolved.put(channelId, maxTier);
            }
        }
        return resolved;
    }

    public static Map<String, Integer> mergeChannelMaxTierMap(Map<String, Integer> base,
        StructureLibPreviewSelection selection) {
        if ((base == null || base.isEmpty()) && (selection == null || selection.getChannelOverrides()
            .isEmpty())) {
            return Map.of();
        }
        LinkedHashMap<String, Integer> merged = new LinkedHashMap<>();
        if (base != null && !base.isEmpty()) {
            merged.putAll(base);
        }
        if (selection != null) {
            for (Map.Entry<String, Integer> entry : selection.getChannelOverrides()
                .entrySet()) {
                String channelId = StructureLibPreviewSelection.normalizeChannelId(entry.getKey());
                Integer selectedValue = entry.getValue();
                if (channelId == null || selectedValue == null || selectedValue <= 0) {
                    continue;
                }
                merged.compute(
                    channelId,
                    (k, knownMax) -> knownMax != null ? Math.max(knownMax, selectedValue) : selectedValue);
            }
        }
        return merged.isEmpty() ? Map.of() : merged;
    }

    public static void collectChannelIds(BuildSnapshot snapshot, Set<String> discoveredChannels) {
        if (snapshot == null || discoveredChannels == null) {
            return;
        }
        if (!snapshot.channelIds.isEmpty()) {
            discoveredChannels.addAll(snapshot.channelIds);
            return;
        }
        collectChannelIds(snapshot.visitedElementsByPos, discoveredChannels);
    }

    public static void collectChannelIds(Map<Long, IStructureElement<?>> visitedElementsByPos,
        Set<String> discoveredChannels) {
        if (visitedElementsByPos == null || visitedElementsByPos.isEmpty()) {
            return;
        }
        for (IStructureElement<?> visitedElement : visitedElementsByPos.values()) {
            String channelId = StructureLibPreviewMetadataFactory.resolveChannelId(visitedElement);
            if (channelId != null) {
                discoveredChannels.add(channelId);
            }
        }
    }

    public static BuildSnapshot buildSnapshot(StructureLibImportRequest request, ResolvedController controller,
        StructureLibPreviewSelection selection, List<String> warnings) {
        try {
            BuildContext context = new BuildContext();
            try {
                return buildSnapshot(request, controller, selection, warnings, context);
            } finally {
                context.clear();
            }
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "Failed to create Guidebook fake world for StructureLib preview", t);
            return BuildSnapshot.failure("StructureLib preview requires an active client world.");
        }
    }

    public static BuildSnapshot buildSnapshot(StructureLibImportRequest request, ResolvedController controller,
        StructureLibPreviewSelection selection, List<String> warnings, BuildContext context) {
        PreparedPreviewWorld prepared = preparePreviewWorld(request, controller, selection, warnings, context);
        if (!prepared.success) {
            return BuildSnapshot.failure(prepared.errorMessage);
        }
        SnapshotBlocksResult snapshotBlocks = snapshotBlocks(prepared.level);
        if (snapshotBlocks.blocks.isEmpty()) {
            context.resetPreviewState();
            return BuildSnapshot.failure("StructureLib preview did not place any blocks.");
        }
        BuildSnapshot snapshot = BuildSnapshot.success(
            snapshotBlocks.blocks,
            snapshotBlocks.absoluteBlocks,
            prepared.visitedElementsByPos,
            buildFingerprint(snapshotBlocks.blocks),
            prepared.world,
            prepared.triggerStack,
            prepared.constructable,
            prepared.actor);
        return snapshot;
    }

    public static BuildSnapshot getOrCreateAnalysisSnapshot(StructureLibImportRequest request,
        ResolvedController controller, StructureLibPreviewSelection selection, BuildContext context) {
        AnalysisSnapshotKey key = new AnalysisSnapshotKey(
            request.getController(),
            request.getPiece(),
            request.getFacing(),
            request.getRotation(),
            request.getFlip(),
            selection);
        AnalysisSnapshot cached = ANALYSIS_SNAPSHOT_CACHE.get(key);
        if (cached != null) {
            return cached.toBuildSnapshot();
        }

        BuildSnapshot built = buildAnalysisSnapshot(request, controller, selection, context);
        AnalysisSnapshot snapshot = AnalysisSnapshot.fromBuildSnapshot(built);
        ANALYSIS_SNAPSHOT_CACHE.put(key, snapshot);
        return snapshot.toBuildSnapshot();
    }

    public static BuildSnapshot buildAnalysisSnapshot(StructureLibImportRequest request, ResolvedController controller,
        StructureLibPreviewSelection selection, BuildContext context) {
        PreparedPreviewWorld prepared = preparePreviewWorld(request, controller, selection, new ArrayList<>(), context);
        if (!prepared.success) {
            return BuildSnapshot.failure(prepared.errorMessage);
        }
        String fingerprint = buildLevelFingerprint(prepared.level);
        if (fingerprint.isEmpty()) {
            context.resetPreviewState();
            return BuildSnapshot.failure("StructureLib preview did not place any blocks.");
        }
        LinkedHashSet<String> channelIds = new LinkedHashSet<>();
        collectChannelIds(prepared.visitedElementsByPos, channelIds);
        return BuildSnapshot.analysis(fingerprint, channelIds);
    }

    public static PreparedPreviewWorld preparePreviewWorld(StructureLibImportRequest request,
        ResolvedController controller, StructureLibPreviewSelection selection, List<String> warnings,
        BuildContext context) {
        context.resetPreviewState();
        GuidebookLevel level = context.getLevel();
        World world = context.getWorld();
        PreviewFakePlayer fakePlayer = context.getFakePlayer();
        TileEntity controllerTile = placeController(level, world, fakePlayer, controller, warnings);
        if (controllerTile == null) {
            context.resetPreviewState();
            return PreparedPreviewWorld.failure(
                "Failed to create a controller tile for " + request.getController() + " in the preview world.");
        }

        StructureLibOrientationHelper.applyDefaultAlignment(controllerTile);
        StructureLibOrientationHelper.applyRequestedAlignment(controllerTile, request, warnings);
        fakePlayer.configureForControllerFacing(StructureLibOrientationHelper.resolveControllerFacing(controllerTile));
        IConstructable constructable = resolveConstructable(controllerTile);
        if (constructable == null) {
            context.resetPreviewState();
            return PreparedPreviewWorld.failure(
                "Failed to resolve a StructureLib constructable for controller " + request.getController() + ".");
        }

        ItemStack triggerStack = createTriggerStack(selection);
        Map<Long, IStructureElement<?>> visitedElementsByPos = Map.of();
        Object instrumentId = new Object();
        StructureLibStructureVisitCollector visitCollector = new StructureLibStructureVisitCollector(
            instrumentId,
            world);
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
            GuideDebugLog.warn(
                LOG,
                "Failed to enable StructureLib instrumentation for controller {}",
                request.getController(),
                t);
        }

        try {
            BuildAttemptResult buildResult = buildConstructable(
                constructable,
                triggerStack,
                fakePlayer,
                selection,
                warnings);
            if (!buildResult.success) {
                context.resetPreviewState();
                return PreparedPreviewWorld.failure(buildResult.errorMessage);
            }
            synchronizePreviewState(controllerTile, triggerStack, selection, warnings);
        } catch (Throwable t) {
            GuideDebugLog.warn(LOG, "StructureLib construct() failed for controller {}", request.getController(), t);
            context.resetPreviewState();
            return PreparedPreviewWorld.failure("StructureLib construct() failed: " + sanitizeMessage(t.getMessage()));
        } finally {
            if (instrumentEnabled) {
                visitedElementsByPos = visitCollector.snapshot();
                MinecraftForge.EVENT_BUS.unregister(visitCollector);
                try {
                    StructureLibAPI.disableInstrument();
                } catch (IllegalStateException ignored) {}
            }
        }

        return PreparedPreviewWorld
            .success(level, world, triggerStack, constructable, fakePlayer, visitedElementsByPos);
    }

    public static TileEntity placeController(GuidebookLevel level, World world, PreviewFakePlayer fakePlayer,
        ResolvedController controller, List<String> warnings) {
        TileEntity tile = placeControllerDirectly(level, world, controller, warnings);
        if (tile != null) {
            return tile;
        }
        warnings.add("Controller direct placement failed in StructureLib preview.");
        return null;
    }

    @Nullable
    public static TileEntity placeControllerDirectly(GuidebookLevel level, World world, ResolvedController controller,
        List<String> warnings) {
        for (StructureLibControllerPlacementIntegration integration : StructureLibControllerIntegrationRegistry.global()
            .placementIntegrations()) {
            try {
                TileEntity integratedTile = integration.placeController(level, world, controller, warnings);
                if (integratedTile != null) {
                    return integratedTile;
                }
            } catch (Throwable t) {
                GuideDebugLog.warn(LOG, "StructureLib controller placement integration failed", t);
            }
        }

        TileEntity tile = null;
        try {
            if (controller.block.hasTileEntity(controller.meta)) {
                tile = controller.block.createTileEntity(world, controller.meta);
            }
        } catch (Throwable t) {
            if (warnings != null) {
                warnings.add("Direct controller tile creation failed for " + controller.blockId + ".");
            }
            GuideDebugLog.warn(LOG, "Direct controller tile creation failed for {}", controller.blockId, t);
            return null;
        }

        if (tile == null) {
            return null;
        }

        level.setBlock(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z, controller.block, controller.meta, tile);
        TileEntity placedTile = world.getTileEntity(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z);
        if (placedTile != null) {
            level.setExplicitBlockId(CONTROLLER_X, CONTROLLER_Y, CONTROLLER_Z, controller.blockId);
            return placedTile;
        }
        return null;
    }

    @Deprecated
    public static void applyRequestedAlignment(TileEntity controllerTile, StructureLibImportRequest request,
        List<String> warnings) {
        StructureLibOrientationHelper.applyRequestedAlignment(controllerTile, request, warnings);
    }

    @Deprecated
    public static void applyDefaultAlignment(TileEntity controllerTile) {
        StructureLibOrientationHelper.applyDefaultAlignment(controllerTile);
    }

    public static ForgeDirection resolveControllerFacing(TileEntity controllerTile) {
        return StructureLibOrientationHelper.resolveControllerFacing(controllerTile);
    }

    @Nullable
    public static IAlignment resolveAlignment(TileEntity controllerTile) {
        return StructureLibOrientationHelper.resolveAlignment(controllerTile);
    }

    @Nullable
    public static IConstructable resolveConstructable(TileEntity controllerTile) {
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
                IAlignment alignment = resolveAlignment(controllerTile);
                ExtendedFacing facing = alignment != null ? alignment.getExtendedFacing() : ExtendedFacing.DEFAULT;
                return container.toConstructable(controllerTile, facing);
            }
        }
        return null;
    }

    public static ItemStack createTriggerStack(StructureLibPreviewSelection selection) {
        StructureLibPreviewSelection effectiveSelection = selection != null ? selection
            : StructureLibPreviewSelection.defaultSelection();
        ItemStack triggerStack = new ItemStack(
            StructureLibAPI.getDefaultHologramItem(),
            Math.max(MIN_TIER, effectiveSelection.getMasterTier()));
        for (Map.Entry<String, Integer> entry : effectiveSelection.getChannelOverrides()
            .entrySet()) {
            Integer channelValue = entry.getValue();
            if (channelValue != null && channelValue > 0) {
                ChannelDataAccessor.setChannelData(triggerStack, entry.getKey(), channelValue);
            }
        }
        for (StructureLibPreviewItemProvider provider : StructureLibControllerIntegrationRegistry.global()
            .previewItemProviders()) {
            provider.configureTrigger(triggerStack, effectiveSelection);
        }
        return triggerStack;
    }

    public static BuildAttemptResult buildConstructable(IConstructable constructable, ItemStack triggerStack,
        PreviewFakePlayer fakePlayer, StructureLibPreviewSelection selection, List<String> warnings) {
        boolean useSurvivalConstruct = selection != null
            && selection.isIntegrationOptionEnabled(StructureLibPreviewSelection.SURVIVAL_CONSTRUCT_OPTION);
        if (useSurvivalConstruct && constructable instanceof ISurvivalConstructable survivalConstructable) {
            boolean fillEmptyHatchesOnly = selection
                .isIntegrationOptionEnabled(StructureLibPreviewSelection.SURVIVAL_FILL_EMPTY_HATCHES_OPTION);
            if (fillEmptyHatchesOnly) {
                constructable.construct(triggerStack.copy(), false);
                ItemStack hatchTriggerStack = triggerStack.copy();
                enablePreviewHatchPlacement(hatchTriggerStack, selection);
                BuildAttemptResult result = runSurvivalConstruct(
                    survivalConstructable,
                    hatchTriggerStack,
                    fakePlayer,
                    warnings);
                if (!result.success && warnings != null) {
                    warnings.add(result.errorMessage + " Keeping the normal StructureLib construct() result.");
                }
                return BuildAttemptResult.success();
            }
            BuildAttemptResult result = runSurvivalConstruct(survivalConstructable, triggerStack, fakePlayer, warnings);
            return result.success ? result
                : fallbackToCreativeConstruct(constructable, triggerStack, warnings, result.errorMessage);
        }
        constructable.construct(triggerStack.copy(), false);
        return BuildAttemptResult.success();
    }

    private static BuildAttemptResult runSurvivalConstruct(ISurvivalConstructable survivalConstructable,
        ItemStack triggerStack, PreviewFakePlayer fakePlayer, List<String> warnings) {
        try {
            ISurvivalBuildEnvironment environment = ISurvivalBuildEnvironment
                .create(StructureLibPreviewItemSource.create(), fakePlayer);
            int rounds = 0;
            while (rounds++ < SURVIVAL_BUILD_MAX_ROUNDS) {
                int result = survivalConstructable
                    .survivalConstruct(triggerStack.copy(), SURVIVAL_BUILD_BUDGET, environment);
                if (result == -1) {
                    return BuildAttemptResult.success();
                }
                if (result == -2) {
                    break;
                }
                if (result <= 0) {
                    return BuildAttemptResult.failure("StructureLib survival construct made no progress.");
                }
            }
            return BuildAttemptResult
                .failure("StructureLib survival construct did not finish within the export budget.");
        } catch (Throwable t) {
            return BuildAttemptResult
                .failure("StructureLib survival construct failed: " + sanitizeMessage(t.getMessage()));
        }
    }

    private static BuildAttemptResult fallbackToCreativeConstruct(IConstructable constructable, ItemStack triggerStack,
        List<String> warnings, String reason) {
        if (warnings != null) {
            warnings.add(reason + " Falling back to normal StructureLib construct().");
        }
        constructable.construct(triggerStack.copy(), false);
        return BuildAttemptResult.success();
    }

    private static void enablePreviewHatchPlacement(ItemStack triggerStack, StructureLibPreviewSelection selection) {
        for (StructureLibPreviewItemProvider provider : StructureLibControllerIntegrationRegistry.global()
            .previewItemProviders()) {
            provider.configureTrigger(
                triggerStack,
                selection.withIntegrationOption(StructureLibPreviewSelection.FORCE_HATCH_PLACEMENT_OPTION, true));
        }
    }

    public static void synchronizePreviewState(TileEntity controllerTile, ItemStack triggerStack,
        StructureLibPreviewSelection selection, List<String> warnings) {
        for (StructureLibPreviewStateSynchronizer synchronizer : StructureLibControllerIntegrationRegistry.global()
            .previewStateSynchronizers()) {
            try {
                synchronizer.synchronizePreviewState(controllerTile, triggerStack, selection, warnings);
            } catch (Throwable t) {
                GuideDebugLog.warn(LOG, "StructureLib preview state synchronizer failed", t);
            }
        }
    }

    public static SnapshotBlocksResult snapshotBlocks(GuidebookLevel level) {
        List<int[]> filledBlocks = new ArrayList<>(level.getFilledBlocks());
        List<AbsolutePlacedBlock> absoluteBlocks = new ArrayList<>(filledBlocks.size());
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (int[] filledBlock : filledBlocks) {
            int x = filledBlock[0];
            int y = filledBlock[1];
            int z = filledBlock[2];
            Block block = level.getBlock(x, y, z);
            if (block == null || block == Blocks.air) {
                continue;
            }
            int meta = level.getBlockMetadata(x, y, z);
            TileEntity tile = level.getTileEntity(x, y, z);
            String blockId = resolvePlacedBlockId(level, x, y, z, block);
            absoluteBlocks.add(new AbsolutePlacedBlock(x, y, z, block, meta, serializeTile(tile), blockId));
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

    public static String buildFingerprint(List<StructureLibImportResult.PlacedBlock> blocks) {
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

    public static String buildLevelFingerprint(GuidebookLevel level) {
        List<int[]> sortedBlocks = new ArrayList<>(level.getFilledBlocks());
        if (sortedBlocks.isEmpty()) {
            return "";
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (int[] filledBlock : sortedBlocks) {
            minX = Math.min(minX, filledBlock[0]);
            minY = Math.min(minY, filledBlock[1]);
            minZ = Math.min(minZ, filledBlock[2]);
        }

        StringBuilder builder = new StringBuilder(sortedBlocks.size() * 24);
        sortedBlocks.sort(
            Comparator.comparingInt((int[] value) -> value[0])
                .thenComparingInt(value -> value[1])
                .thenComparingInt(value -> value[2]));
        for (int[] filledBlock : sortedBlocks) {
            int x = filledBlock[0];
            int y = filledBlock[1];
            int z = filledBlock[2];
            Block block = level.getBlock(x, y, z);
            if (block == null || block == Blocks.air) {
                continue;
            }
            builder.append(x - minX)
                .append(',')
                .append(y - minY)
                .append(',')
                .append(z - minZ)
                .append(':')
                .append(resolvePlacedBlockId(level, x, y, z, block))
                .append('@')
                .append(level.getBlockMetadata(x, y, z))
                .append(';');
        }
        return builder.toString();
    }

    @Nullable
    private static String resolvePlacedBlockId(GuidebookLevel level, int x, int y, int z, Block block) {
        String explicitBlockId = level.getExplicitBlockId(x, y, z);
        return explicitBlockId != null ? explicitBlockId : resolveBlockId(block);
    }

    public static List<String> mergeWarnings(List<String> leadingWarnings, List<String> cachedWarnings) {
        if ((leadingWarnings == null || leadingWarnings.isEmpty())
            && (cachedWarnings == null || cachedWarnings.isEmpty())) {
            return List.of();
        }
        ArrayList<String> merged = new ArrayList<>();
        if (leadingWarnings != null) {
            merged.addAll(leadingWarnings);
        }
        if (cachedWarnings != null) {
            merged.addAll(cachedWarnings);
        }
        return merged;
    }

    @Nullable
    public static NBTTagCompound serializeTile(@Nullable TileEntity tile) {
        if (tile == null) {
            return null;
        }
        try {
            NBTTagCompound tag = new NBTTagCompound();
            tile.writeToNBT(tag);
            return tag;
        } catch (Throwable t) {
            GuideDebugLog.warn(
                LOG,
                "Failed to serialize preview tile entity {}",
                tile.getClass()
                    .getName(),
                t);
            return null;
        }
    }

    @Nullable
    public static String resolveBlockId(@Nullable Block block) {
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
        return null;
    }

    public static String sanitizeMessage(@Nullable String message) {
        if (message == null) {
            return "unknown error";
        }
        String trimmed = message.trim();
        return trimmed.isEmpty() ? "unknown error" : trimmed;
    }

    public static ResolvedController resolveController(StructureLibImportRequest request) {
        GuideBlockMatcher matcher = GuideBlockMatcher.parse(request.getController());
        Block block = (Block) Block.blockRegistry.getObject(matcher.getBlockId());
        if (block == null || block == Blocks.air) {
            throw new IllegalArgumentException(
                "Could not resolve StructureLib controller block: " + request.getController());
        }
        return new ResolvedController(matcher.getBlockId(), block, matcher.getMeta() != null ? matcher.getMeta() : 0);
    }

    public static ForgeDirection parseDirection(@Nullable String rawFacing, List<String> warnings) {
        return StructureLibOrientationHelper.parseDirection(rawFacing, warnings);
    }

    public static Rotation parseRotation(@Nullable String rawRotation, List<String> warnings) {
        return StructureLibOrientationHelper.parseRotation(rawRotation, warnings);
    }

    public static Flip parseFlip(@Nullable String rawFlip, List<String> warnings) {
        return StructureLibOrientationHelper.parseFlip(rawFlip, warnings);
    }

    public static String normalizeRotation(String rawRotation) {
        return StructureLibOrientationHelper.normalizeRotation(rawRotation);
    }

    public static String normalizeFlip(String rawFlip) {
        return StructureLibOrientationHelper.normalizeFlip(rawFlip);
    }

    @Nullable
    public static String normalizeBlockId(@Nullable String blockId) {
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

    public static int clamp(int value, int minValue, int maxValue) {
        if (value < minValue) {
            return minValue;
        }
        return Math.min(value, maxValue);
    }

    public static class AnalysisKey {

        private final String controller;
        @Nullable
        private final String piece;
        @Nullable
        private final String facing;
        @Nullable
        private final String rotation;
        @Nullable
        private final String flip;

        private AnalysisKey(String controller, @Nullable String piece, @Nullable String facing,
            @Nullable String rotation, @Nullable String flip) {
            this.controller = controller;
            this.piece = piece;
            this.facing = facing;
            this.rotation = rotation;
            this.flip = flip;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AnalysisKey other)) {
                return false;
            }
            return controller.equals(other.controller) && Objects.equals(piece, other.piece)
                && Objects.equals(facing, other.facing)
                && Objects.equals(rotation, other.rotation)
                && Objects.equals(flip, other.flip);
        }

        @Override
        public int hashCode() {
            return Objects.hash(controller, piece, facing, rotation, flip);
        }
    }

    public static class ControlAnalysis {

        private final int maxTotalTier;
        private final Map<String, Integer> channelMaxTierMap;

        private ControlAnalysis(int maxTotalTier, Map<String, Integer> channelMaxTierMap) {
            this.maxTotalTier = Math.max(MIN_TIER, maxTotalTier);
            this.channelMaxTierMap = immutableChannelMaxTierMap(channelMaxTierMap);
        }

        public int getMaxTotalTier() {
            return maxTotalTier;
        }

        public Map<String, Integer> getChannelMaxTierMap() {
            return channelMaxTierMap;
        }

        public StructureLibPreviewSelection clampSelection(StructureLibPreviewSelection selection) {
            StructureLibPreviewSelection effectiveSelection = selection != null ? selection
                : StructureLibPreviewSelection.defaultSelection();
            LinkedHashMap<String, Integer> clampedChannels = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : effectiveSelection.getChannelOverrides()
                .entrySet()) {
                Integer maxValue = channelMaxTierMap.get(entry.getKey());
                if (maxValue == null || maxValue <= 0 || entry.getValue() == null) {
                    continue;
                }
                int clamped = clamp(entry.getValue(), 1, maxValue);
                clampedChannels.put(entry.getKey(), clamped);
            }
            return new StructureLibPreviewSelection(
                clamp(effectiveSelection.getMasterTier(), MIN_TIER, maxTotalTier),
                clampedChannels,
                effectiveSelection.getIntegrationOptions());
        }

        public static Map<String, Integer> immutableChannelMaxTierMap(@Nullable Map<String, Integer> source) {
            if (source == null || source.isEmpty()) {
                return Map.of();
            }
            LinkedHashMap<String, Integer> normalized = new LinkedHashMap<>(source.size());
            for (Map.Entry<String, Integer> entry : source.entrySet()) {
                String channelId = StructureLibPreviewSelection.normalizeChannelId(entry.getKey());
                Integer value = entry.getValue();
                if (channelId == null || value == null || value <= 0) {
                    continue;
                }
                normalized.put(channelId, value);
            }
            return normalized.isEmpty() ? Map.of() : Map.copyOf(normalized);
        }
    }

    public static class AnalysisSnapshotKey {

        private final String controller;
        @Nullable
        private final String piece;
        @Nullable
        private final String facing;
        @Nullable
        private final String rotation;
        @Nullable
        private final String flip;
        private final StructureLibPreviewSelection selection;

        public AnalysisSnapshotKey(String controller, @Nullable String piece, @Nullable String facing,
            @Nullable String rotation, @Nullable String flip, StructureLibPreviewSelection selection) {
            this.controller = controller;
            this.piece = piece;
            this.facing = facing;
            this.rotation = rotation;
            this.flip = flip;
            this.selection = selection != null ? selection : StructureLibPreviewSelection.defaultSelection();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof AnalysisSnapshotKey other)) {
                return false;
            }
            return controller.equals(other.controller) && Objects.equals(piece, other.piece)
                && Objects.equals(facing, other.facing)
                && Objects.equals(rotation, other.rotation)
                && Objects.equals(flip, other.flip)
                && selection.equals(other.selection);
        }

        @Override
        public int hashCode() {
            return Objects.hash(controller, piece, facing, rotation, flip, selection);
        }
    }

    public static class AnalysisSnapshot {

        private final boolean success;
        private final Set<String> channelIds;
        private final String fingerprint;
        @Nullable
        private final String errorMessage;

        public AnalysisSnapshot(boolean success, Set<String> channelIds, String fingerprint,
            @Nullable String errorMessage) {
            this.success = success;
            this.channelIds = channelIds != null ? Set.copyOf(new LinkedHashSet<>(channelIds)) : Set.of();
            this.fingerprint = fingerprint != null ? fingerprint : "";
            this.errorMessage = errorMessage;
        }

        public static AnalysisSnapshot fromBuildSnapshot(BuildSnapshot snapshot) {
            return new AnalysisSnapshot(
                snapshot.success,
                snapshot.channelIds,
                snapshot.fingerprint,
                snapshot.errorMessage);
        }

        public BuildSnapshot toBuildSnapshot() {
            if (!success) {
                return BuildSnapshot.failure(errorMessage);
            }
            return BuildSnapshot.analysis(fingerprint, channelIds);
        }
    }

    public static class ResolvedController {

        private final String blockId;
        private final Block block;
        private final int meta;

        public ResolvedController(String blockId, Block block, int meta) {
            this.blockId = blockId;
            this.block = block;
            this.meta = meta;
        }

        public String getBlockId() {
            return blockId;
        }

        public Block getBlock() {
            return block;
        }

        public int getMeta() {
            return meta;
        }
    }

    public static class BuildContext {

        private final GuidebookLevel level;
        private final World world;
        private final PreviewFakePlayer fakePlayer;

        public BuildContext() {
            level = new GuidebookLevel();
            world = level.getOrCreateFakeWorld();
            fakePlayer = new PreviewFakePlayer(world);
        }

        public GuidebookLevel getLevel() {
            return level;
        }

        public World getWorld() {
            return world;
        }

        public PreviewFakePlayer getFakePlayer() {
            return fakePlayer;
        }

        public void resetPreviewState() {
            level.clear();
            fakePlayer.inventory.clearInventory(null, -1);
        }

        public void clear() {
            resetPreviewState();
        }
    }

    public static class AbsolutePlacedBlock {

        private final int x;
        private final int y;
        private final int z;
        private final Block block;
        private final int meta;
        @Nullable
        private final NBTTagCompound tileTag;
        @Nullable
        private final String blockId;

        public AbsolutePlacedBlock(int x, int y, int z, Block block, int meta, @Nullable NBTTagCompound tileTag,
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

    public static class PreparedPreviewWorld {

        private final boolean success;
        private final GuidebookLevel level;
        @Nullable
        private final World world;
        private final ItemStack triggerStack;
        @Nullable
        private final Object constructable;
        @Nullable
        private final EntityPlayer actor;
        private final Map<Long, IStructureElement<?>> visitedElementsByPos;
        @Nullable
        private final String errorMessage;

        public PreparedPreviewWorld(boolean success, GuidebookLevel level, @Nullable World world,
            ItemStack triggerStack, @Nullable Object constructable, @Nullable EntityPlayer actor,
            Map<Long, IStructureElement<?>> visitedElementsByPos, @Nullable String errorMessage) {
            this.success = success;
            this.level = level;
            this.world = world;
            this.triggerStack = triggerStack;
            this.constructable = constructable;
            this.actor = actor;
            this.visitedElementsByPos = visitedElementsByPos;
            this.errorMessage = errorMessage;
        }

        public static PreparedPreviewWorld success(GuidebookLevel level, World world, ItemStack triggerStack,
            Object constructable, EntityPlayer actor, Map<Long, IStructureElement<?>> visitedElementsByPos) {
            return new PreparedPreviewWorld(
                true,
                level,
                world,
                triggerStack,
                constructable,
                actor,
                visitedElementsByPos,
                null);
        }

        public static PreparedPreviewWorld failure(String errorMessage) {
            return new PreparedPreviewWorld(
                false,
                new GuidebookLevel(),
                null,
                new ItemStack(StructureLibAPI.getDefaultHologramItem(), MIN_TIER),
                null,
                null,
                Map.of(),
                sanitizeMessage(errorMessage));
        }
    }

    public static class BuildSnapshot {

        private final boolean success;
        private final List<StructureLibImportResult.PlacedBlock> blocks;
        private final List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks;
        private final Map<Long, IStructureElement<?>> visitedElementsByPos;
        private final Set<String> channelIds;
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

        public BuildSnapshot(boolean success, List<StructureLibImportResult.PlacedBlock> blocks,
            List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks,
            Map<Long, IStructureElement<?>> visitedElementsByPos, Set<String> channelIds, String fingerprint,
            @Nullable World world, ItemStack triggerStack, @Nullable Object constructable, @Nullable EntityPlayer actor,
            @Nullable String errorMessage) {
            this.success = success;
            this.blocks = blocks;
            this.absoluteBlocks = absoluteBlocks;
            this.visitedElementsByPos = visitedElementsByPos;
            this.channelIds = channelIds != null ? Set.copyOf(new LinkedHashSet<>(channelIds)) : Set.of();
            this.fingerprint = fingerprint;
            this.world = world;
            this.triggerStack = triggerStack;
            this.constructable = constructable;
            this.actor = actor;
            this.errorMessage = errorMessage;
        }

        public static BuildSnapshot success(List<StructureLibImportResult.PlacedBlock> blocks,
            List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks,
            Map<Long, IStructureElement<?>> visitedElementsByPos, String fingerprint, World world,
            ItemStack triggerStack, Object constructable, EntityPlayer actor) {
            return success(
                blocks,
                absoluteBlocks,
                visitedElementsByPos,
                Set.of(),
                fingerprint,
                world,
                triggerStack,
                constructable,
                actor);
        }

        public static BuildSnapshot success(List<StructureLibImportResult.PlacedBlock> blocks,
            List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks,
            Map<Long, IStructureElement<?>> visitedElementsByPos, Set<String> channelIds, String fingerprint,
            World world, ItemStack triggerStack, Object constructable, EntityPlayer actor) {
            return new BuildSnapshot(
                true,
                blocks,
                absoluteBlocks,
                visitedElementsByPos,
                channelIds,
                fingerprint,
                world,
                triggerStack,
                constructable,
                actor,
                null);
        }

        public static BuildSnapshot analysis(String fingerprint, Set<String> channelIds) {
            return new BuildSnapshot(
                true,
                List.of(),
                List.of(),
                Map.of(),
                channelIds,
                fingerprint,
                null,
                new ItemStack(StructureLibAPI.getDefaultHologramItem(), MIN_TIER),
                null,
                null,
                null);
        }

        public static BuildSnapshot failure(String errorMessage) {
            return new BuildSnapshot(
                false,
                List.of(),
                List.of(),
                Map.of(),
                Set.of(),
                "",
                null,
                new ItemStack(StructureLibAPI.getDefaultHologramItem(), MIN_TIER),
                null,
                null,
                errorMessage);
        }
    }

    public static class BuildAttemptResult {

        private final boolean success;
        private final String errorMessage;

        public BuildAttemptResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static BuildAttemptResult success() {
            return new BuildAttemptResult(true, "");
        }

        public static BuildAttemptResult failure(String errorMessage) {
            return new BuildAttemptResult(false, sanitizeMessage(errorMessage));
        }
    }

    public static class SnapshotBlocksResult {

        public static final SnapshotBlocksResult EMPTY = new SnapshotBlocksResult(List.of(), List.of());

        private final List<StructureLibImportResult.PlacedBlock> blocks;
        private final List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks;

        public SnapshotBlocksResult(List<StructureLibImportResult.PlacedBlock> blocks,
            List<StructureLibPreviewMetadataFactory.AbsolutePreviewBlock> absoluteBlocks) {
            this.blocks = blocks;
            this.absoluteBlocks = absoluteBlocks;
        }

        public static SnapshotBlocksResult empty() {
            return EMPTY;
        }
    }

    public static class PreviewFakePlayer extends EntityPlayer {

        public PreviewFakePlayer(World world) {
            super(world, new GameProfile(UUID.fromString("9c7ef542-6ab6-4524-b7d7-8caaf8df467c"), "GuideNHPreview"));
            capabilities.isCreativeMode = true;
            noClip = true;
            configureForControllerFacing(ForgeDirection.SOUTH);
        }

        public void configureForControllerFacing(ForgeDirection controllerFacing) {
            ForgeDirection facing = controllerFacing != null && controllerFacing != ForgeDirection.UNKNOWN
                ? controllerFacing
                : ForgeDirection.SOUTH;
            double x = CONTROLLER_X + 0.5D + facing.offsetX * 4.0D;
            double y = CONTROLLER_Y + 1.62D;
            double z = CONTROLLER_Z + 0.5D + facing.offsetZ * 4.0D;
            float yaw = yawForFacing(facing);
            prevPosX = lastTickPosX = posX = x;
            prevPosY = lastTickPosY = posY = y;
            prevPosZ = lastTickPosZ = posZ = z;
            prevRotationYaw = rotationYaw = yaw;
            prevRotationPitch = rotationPitch = 0.0F;
            setPositionAndRotation(x, y, z, yaw, 0.0F);
        }

        private static float yawForFacing(ForgeDirection facing) {
            return switch (facing) {
                case EAST -> 90.0F;
                case SOUTH -> 180.0F;
                case WEST -> 270.0F;
                default -> 0.0F;
            };
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
