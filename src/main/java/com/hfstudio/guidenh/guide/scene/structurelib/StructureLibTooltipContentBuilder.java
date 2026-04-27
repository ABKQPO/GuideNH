package com.hfstudio.guidenh.guide.scene.structurelib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytSlotGrid;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;

public class StructureLibTooltipContentBuilder {

    private static final int DEFAULT_CANDIDATE_COLUMNS = 6;

    private StructureLibTooltipContentBuilder() {}

    public static ContentTooltip build(String blockName, @Nullable String structureLibDescription, boolean shiftDown,
        List<ItemStack> blockCandidates, List<String> hatchDescriptionLines, List<ItemStack> hatchCandidates) {
        LytVBox root = new LytVBox();
        root.setGap(2);
        root.append(LytParagraph.of(requireBlockName(blockName)));

        List<ItemStack> normalizedBlockCandidates = normalizeStacks(blockCandidates);
        List<ItemStack> normalizedHatchCandidates = normalizeStacks(hatchCandidates);
        boolean hasCandidates = !normalizedBlockCandidates.isEmpty() || !normalizedHatchCandidates.isEmpty();
        String normalizedStructureLibDescription = normalizeLine(structureLibDescription);
        if (hasCandidates && !shiftDown) {
            root.append(LytParagraph.of(GuidebookText.SceneStructureLibHoldShiftCandidates.text()));
        } else if (normalizedStructureLibDescription != null
            && !isGenericStructureLibDescription(normalizedStructureLibDescription)) {
            root.append(LytParagraph.of(normalizedStructureLibDescription));
        }

        appendDescriptionLines(root, hatchDescriptionLines);

        if (shiftDown) {
            appendCandidateGrid(root, normalizedBlockCandidates);
            appendCandidateGrid(root, normalizedHatchCandidates);
        }

        return new ContentTooltip(root);
    }

    private static void appendDescriptionLines(LytVBox root, @Nullable List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        for (String line : lines) {
            String normalized = normalizeLine(line);
            if (normalized != null) {
                root.append(LytParagraph.of(normalized));
            }
        }
    }

    private static void appendCandidateGrid(LytVBox root, List<ItemStack> candidates) {
        if (candidates.isEmpty()) {
            return;
        }
        int columns = Math.max(1, resolveCandidateColumns());
        int width = Math.min(columns, candidates.size());
        int height = (candidates.size() + width - 1) / width;
        LytSlotGrid grid = new LytSlotGrid(width, height);
        grid.setRenderEmptySlots(false);
        grid.setRenderSlotBackground(false);
        for (int i = 0; i < candidates.size(); i++) {
            grid.setItem(i % width, i / width, candidates.get(i));
        }
        root.append(grid);
    }

    private static int resolveCandidateColumns() {
        try {
            return ModConfig.ui.sceneStructureLibCandidateColumns;
        } catch (Throwable ignored) {
            return DEFAULT_CANDIDATE_COLUMNS;
        }
    }

    private static List<ItemStack> normalizeStacks(@Nullable List<ItemStack> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }
        List<ItemStack> normalized = new ArrayList<>(candidates.size());
        for (ItemStack stack : candidates) {
            if (stack != null && stack.stackSize > 0) {
                normalized.add(stack);
            }
        }
        return normalized.isEmpty() ? Collections.<ItemStack>emptyList() : normalized;
    }

    private static String requireBlockName(@Nullable String blockName) {
        String normalized = normalizeLine(blockName);
        if (normalized == null) {
            throw new IllegalArgumentException("StructureLib tooltip block name cannot be empty");
        }
        return normalized;
    }

    private static boolean isGenericStructureLibDescription(String value) {
        return "StructureLib".equalsIgnoreCase(value);
    }

    @Nullable
    private static String normalizeLine(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
