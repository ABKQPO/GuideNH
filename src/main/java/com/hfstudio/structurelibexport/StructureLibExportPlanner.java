package com.hfstudio.structurelibexport;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandException;

import com.hfstudio.guidenh.integration.structurelib.StructureLibImportRequest;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade.ControlAnalysis;
import com.hfstudio.guidenh.integration.structurelib.StructureLibRuntimeFacade.ResolvedController;

public class StructureLibExportPlanner {

    public static final int DEFAULT_MAX_TASKS = 512;
    public static final int DEFAULT_AUTO_TIER_TASK_LIMIT = 100;

    private final StructureLibAlignmentResolver alignmentResolver;

    public StructureLibExportPlanner() {
        this(new StructureLibAlignmentResolver());
    }

    public StructureLibExportPlanner(StructureLibAlignmentResolver alignmentResolver) {
        this.alignmentResolver = alignmentResolver != null ? alignmentResolver : new StructureLibAlignmentResolver();
    }

    public List<StructureLibExportTaskSpec> plan(List<StructureLibControllerSpec> controllers,
        StructureLibExportOptions options) throws CommandException {
        ArrayList<StructureLibExportTaskSpec> tasks = new ArrayList<>();
        for (StructureLibControllerSpec controller : controllers) {
            ArrayList<String> warnings = new ArrayList<>();
            List<StructureLibOrientationSpec> orientations;
            try {
                orientations = alignmentResolver.resolveOrientations(controller, options.getOrientations(), warnings);
            } catch (CommandException e) {
                if (options.getController() != null) {
                    throw e;
                }
                continue;
            }
            for (StructureLibOrientationSpec orientation : orientations) {
                AutoTierPlan autoTierPlan = resolveAutoTierPlan(controller, orientation, options, warnings);
                for (Integer tier : resolveTiers(options, autoTierPlan, warnings)) {
                    Map<String, List<Integer>> channels = resolveChannels(
                        options,
                        tier != null ? tier : 1,
                        autoTierPlan);
                    appendChannelCombinations(
                        tasks,
                        controller,
                        orientation,
                        tier != null ? tier : 1,
                        options,
                        warnings,
                        new LinkedHashMap<>(),
                        new ArrayList<>(channels.entrySet()),
                        0);
                }
            }
        }
        if (tasks.isEmpty()) {
            throw new CommandException("No StructureLib export tasks could be planned for the requested options.");
        }
        if (tasks.size() > DEFAULT_MAX_TASKS && !options.isForce()) {
            throw new CommandException(
                "StructureLib export planned " + tasks.size()
                    + " screenshots. Use --force to allow more than "
                    + DEFAULT_MAX_TASKS
                    + ".");
        }
        return tasks;
    }

    private AutoTierPlan resolveAutoTierPlan(StructureLibControllerSpec controller,
        StructureLibOrientationSpec orientation, StructureLibExportOptions options, List<String> warnings) {
        if (options.isTierExplicit() && options.isChannelsExplicit()) {
            return AutoTierPlan.empty();
        }
        try {
            StructureLibImportRequest request = new StructureLibImportRequest(
                controller.getControllerArgument(),
                null,
                orientation.getFacing(),
                orientation.getRotation(),
                orientation.getFlip(),
                1,
                null);
            ResolvedController resolvedController = new ResolvedController(
                controller.getBlockId(),
                controller.getBlock(),
                controller.getMeta());
            ControlAnalysis analysis = StructureLibRuntimeFacade.analyzeControls(request, resolvedController);
            return new AutoTierPlan(resolveUnifiedMaxTier(analysis), analysis.getChannelMaxTierMap());
        } catch (Throwable t) {
            warnings.add(
                "Could not inspect StructureLib tier/channel ranges for " + controller.getControllerArgument()
                    + "; default export used tier 1.");
            return AutoTierPlan.empty();
        }
    }

    private int resolveUnifiedMaxTier(ControlAnalysis analysis) {
        int maxTier = Math.max(1, analysis.getMaxTotalTier());
        for (Integer channelMaxTier : analysis.getChannelMaxTierMap()
            .values()) {
            if (channelMaxTier != null) {
                maxTier = Math.max(maxTier, channelMaxTier);
            }
        }
        return maxTier;
    }

    private List<Integer> resolveTiers(StructureLibExportOptions options, AutoTierPlan autoTierPlan,
        List<String> warnings) {
        if (options.isTierExplicit()) {
            return options.getTiers();
        }
        ArrayList<Integer> tiers = new ArrayList<>();
        int maxTier = Math.max(1, autoTierPlan.maxTier);
        int cappedMaxTier = Math.min(DEFAULT_AUTO_TIER_TASK_LIMIT, maxTier);
        for (int tier = 1; tier <= cappedMaxTier; tier++) {
            tiers.add(tier);
        }
        if (maxTier > DEFAULT_AUTO_TIER_TASK_LIMIT) {
            warnings.add(
                "Default StructureLib tier export was capped at " + DEFAULT_AUTO_TIER_TASK_LIMIT
                    + " screenshots; use --tier to export a narrower or explicit range.");
        }
        return tiers;
    }

    private Map<String, List<Integer>> resolveChannels(StructureLibExportOptions options, int tier,
        AutoTierPlan autoTierPlan) {
        if (options.isChannelsExplicit()) {
            return options.getChannels();
        }
        LinkedHashMap<String, List<Integer>> channels = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : autoTierPlan.channelMaxTierMap.entrySet()) {
            int value = Math.clamp(tier, 1, Math.max(1, entry.getValue()));
            ArrayList<Integer> values = new ArrayList<>();
            values.add(value);
            channels.put(entry.getKey(), values);
        }
        return channels;
    }

    private void appendChannelCombinations(List<StructureLibExportTaskSpec> tasks,
        StructureLibControllerSpec controller, StructureLibOrientationSpec orientation, int tier,
        StructureLibExportOptions options, List<String> warnings, Map<String, Integer> currentChannels,
        List<Map.Entry<String, List<Integer>>> channelEntries, int index) {
        if (index >= channelEntries.size()) {
            tasks.add(
                new StructureLibExportTaskSpec(
                    controller,
                    orientation,
                    tier,
                    currentChannels,
                    options.getLayerExpression(),
                    options.isLayersEach(),
                    options.getView(),
                    options.getBackground(),
                    options.getPixelsPerBlock(),
                    options.getScale(),
                    options.isGtActiveController(),
                    options.isGtPlaceHatches(),
                    warnings));
            return;
        }
        Map.Entry<String, List<Integer>> entry = channelEntries.get(index);
        for (Integer value : entry.getValue()) {
            LinkedHashMap<String, Integer> next = new LinkedHashMap<>(currentChannels);
            next.put(entry.getKey(), value != null ? value : 1);
            appendChannelCombinations(
                tasks,
                controller,
                orientation,
                tier,
                options,
                warnings,
                next,
                channelEntries,
                index + 1);
        }
    }

    public static class AutoTierPlan {

        private final int maxTier;
        private final Map<String, Integer> channelMaxTierMap;

        public AutoTierPlan(int maxTier, Map<String, Integer> channelMaxTierMap) {
            this.maxTier = Math.max(1, maxTier);
            this.channelMaxTierMap = channelMaxTierMap != null ? channelMaxTierMap : new LinkedHashMap<>();
        }

        public static AutoTierPlan empty() {
            return new AutoTierPlan(1, new LinkedHashMap<>());
        }
    }
}
