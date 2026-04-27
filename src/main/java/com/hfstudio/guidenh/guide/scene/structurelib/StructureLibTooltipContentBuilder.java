package com.hfstudio.guidenh.guide.scene.structurelib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.document.block.LytParagraph;
import com.hfstudio.guidenh.guide.document.block.LytSlotGrid;
import com.hfstudio.guidenh.guide.document.block.LytVBox;
import com.hfstudio.guidenh.guide.document.flow.LytFlowSpan;
import com.hfstudio.guidenh.guide.document.interaction.ContentTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.style.TextStyle;

public class StructureLibTooltipContentBuilder {

    private static final int DEFAULT_CANDIDATE_COLUMNS = 6;
    private static final TextStyle HATCH_LABEL_STYLE = TextStyle.builder()
        .color(new ConstantColor(0xFFFFCC55))
        .build();
    private static final int[] HINT_DOT_COLORS = new int[] { 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00,
        0xFFFF00FF, 0xFF00FFFF, 0xFFFFA500, 0xFF800080, 0xFF006400, 0xFF8B0000, 0xFF00008B, 0xFF008B8B };

    private StructureLibTooltipContentBuilder() {}

    public static ContentTooltip build(String blockName, @Nullable String structureLibDescription, boolean shiftDown,
        List<ItemStack> blockCandidates, List<StructureLibHatchDescriptionLine> hatchDescriptionLines,
        List<ItemStack> hatchCandidates) {
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

    private static void appendDescriptionLines(LytVBox root, @Nullable List<StructureLibHatchDescriptionLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        for (StructureLibHatchDescriptionLine line : lines) {
            LytParagraph paragraph = createDescriptionParagraph(line);
            if (paragraph != null && !paragraph.isEmpty()) {
                root.append(paragraph);
            }
        }
    }

    @Nullable
    private static LytParagraph createDescriptionParagraph(@Nullable StructureLibHatchDescriptionLine line) {
        if (line == null) {
            return null;
        }
        return switch (line.getKind()) {
            case HINT_BLOCK -> createHintBlockParagraph(line.getHintDot());
            case VALID_HATCHES -> createValidHatchesParagraph(line.getText());
        };
    }

    @Nullable
    private static LytParagraph createHintBlockParagraph(int hintDot) {
        if (hintDot <= 0) {
            return null;
        }
        LytParagraph paragraph = new LytParagraph();
        appendStyledText(paragraph, GuidebookText.SceneStructureLibHintBlockLabel.text(), HATCH_LABEL_STYLE);
        appendStyledText(paragraph, GuidebookText.SceneStructureLibHintDotNumber.text(Integer.valueOf(hintDot)),
            TextStyle.builder().color(new ConstantColor(resolveHintDotColor(hintDot))).build());
        return paragraph;
    }

    @Nullable
    private static LytParagraph createValidHatchesParagraph(@Nullable String text) {
        String normalized = normalizeLine(text);
        if (normalized == null) {
            return null;
        }
        LytParagraph paragraph = new LytParagraph();
        appendStyledText(paragraph, GuidebookText.SceneStructureLibValidHatchesLabel.text(), HATCH_LABEL_STYLE);
        appendStyledText(paragraph, normalized, null);
        return paragraph;
    }

    private static void appendStyledText(LytParagraph paragraph, @Nullable String text, @Nullable TextStyle style) {
        if (paragraph == null || text == null || text.isEmpty()) {
            return;
        }
        if (style == null) {
            paragraph.appendText(text);
            return;
        }
        LytFlowSpan span = new LytFlowSpan();
        span.setStyle(style);
        span.appendText(text);
        paragraph.append(span);
    }

    private static int resolveHintDotColor(int hintDot) {
        if (hintDot <= 0) {
            return HINT_DOT_COLORS[0];
        }
        return HINT_DOT_COLORS[(hintDot - 1) % HINT_DOT_COLORS.length];
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
