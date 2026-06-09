package com.hfstudio.guidenh.guide.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import net.minecraft.client.gui.FontRenderer;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuidePage;
import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.LightDarkMode;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.block.LytDocument;
import com.hfstudio.guidenh.guide.document.block.LytHeading;
import com.hfstudio.guidenh.guide.render.GuideTextRenderer;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public class GuideScreenScrollbarOutline {

    public static final long HOVER_DELAY_MILLIS = 500L;

    private static final int H1_MARKER_WIDTH = 10;
    private static final int H2_MARKER_WIDTH = 7;
    private static final int MARKER_HEIGHT = 2;
    private static final int HIT_PADDING_X = 2;
    private static final int HIT_PADDING_Y = 2;
    private static final int LABEL_MAX_LINES = 2;
    private static final String ELLIPSIS = "...";

    private List<OutlineAnchor> anchors = List.of();
    private List<HeadingEntry> entries = List.of();
    private int lastLayoutSignature;
    @Nullable
    private HoverState hoverState;

    public record ScrollbarBounds(int x, int y, int width, int height, int contentHeight, int viewportHeight,
        int maxScroll) {}

    public record HeadingEntry(String text, int depth, int documentY, int colorArgb, int markerX, int markerY,
        int markerWidth, int markerHeight, int hitX, int hitY, int hitWidth, int hitHeight) {}

    public record LabelLayout(List<String> lines, int x, int y, int width, int height, int alpha) {}

    public record RenderState(List<HeadingEntry> entries, int activeIndex, @Nullable Integer hoveredIndex,
        @Nullable LabelLayout labelLayout) {}

    public void invalidateLayout() {
        lastLayoutSignature = 0;
        entries = List.of();
        anchors = List.of();
        hoverState = null;
    }

    public void clearHover() {
        hoverState = null;
    }

    public RenderState update(@Nullable GuidePage page, @Nullable LytDocument document, ScrollbarBounds bounds,
        float zoom, int viewportTopDocumentY, int mouseX, int mouseY, long nowMillis,
        @Nullable FontRenderer fontRenderer, int labelMaxWidth, int labelGap, int panelLeft, int panelRight) {
        rebuildIfNeeded(page, document, bounds, zoom, viewportTopDocumentY);
        updateHoverCandidate(mouseX, mouseY, nowMillis);
        int activeIndex = resolveActiveIndex(viewportTopDocumentY);
        Integer hoveredIndex = hoverState != null ? hoverState.index() : null;
        LabelLayout labelLayout = hoveredIndex != null && fontRenderer != null ? buildLabelLayout(
            entries.get(hoveredIndex),
            fontRenderer,
            nowMillis,
            labelMaxWidth,
            labelGap,
            panelLeft,
            panelRight) : null;
        return new RenderState(entries, activeIndex, hoveredIndex, labelLayout);
    }

    public @Nullable Integer findJumpTarget(int mouseX, int mouseY) {
        for (var entry : entries) {
            if (contains(entry, mouseX, mouseY)) {
                clearHover();
                return entry.documentY();
            }
        }
        return null;
    }

    public int resolveActiveIndex(int viewportTopDocumentY) {
        int activeIndex = -1;
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            if (entry.documentY() > viewportTopDocumentY) {
                break;
            }
            activeIndex = index;
        }
        return activeIndex >= 0 ? activeIndex : entries.isEmpty() ? -1 : 0;
    }

    public void updateHoverCandidateForTest(int mouseX, int mouseY, long nowMillis) {
        updateHoverCandidate(mouseX, mouseY, nowMillis);
    }

    public void setEntriesForTest(List<HeadingEntry> testEntries) {
        entries = List.copyOf(testEntries);
    }

    public void setAnchorsForTest(List<OutlineAnchor> testAnchors) {
        anchors = List.copyOf(testAnchors);
    }

    public HeadingEntry testEntry(String text, int depth, int documentY) {
        return testEntry(text, depth, documentY, 0xFFFFFFFF, 100, 50, markerWidth(depth), MARKER_HEIGHT, 98, 48, 16, 6);
    }

    public HeadingEntry testEntry(String text, int depth, int documentY, int colorArgb, int markerX, int markerY,
        int markerWidth, int markerHeight, int hitX, int hitY, int hitWidth, int hitHeight) {
        return new HeadingEntry(
            text,
            depth,
            documentY,
            colorArgb,
            markerX,
            markerY,
            markerWidth,
            markerHeight,
            hitX,
            hitY,
            hitWidth,
            hitHeight);
    }

    public OutlineAnchor testAnchor(String text, int depth, int documentY, int colorArgb) {
        return new OutlineAnchor(text, depth, documentY, colorArgb);
    }

    public RenderState rebuildFromAnchorsForTest(ScrollbarBounds bounds) {
        entries = mapAnchorsToEntries(anchors, bounds);
        return new RenderState(entries, -1, null, null);
    }

    @Nullable
    public LabelLayout visibleHoverLabelForTest(long nowMillis) {
        if (hoverState == null || hoverState.index() < 0 || hoverState.index() >= entries.size()) {
            return null;
        }
        if (nowMillis - hoverState.startedAtMillis() < HOVER_DELAY_MILLIS) {
            return null;
        }
        return new LabelLayout(
            List.of(
                entries.get(hoverState.index())
                    .text()),
            0,
            0,
            0,
            0,
            255);
    }

    private void rebuildIfNeeded(@Nullable GuidePage page, @Nullable LytDocument document, ScrollbarBounds bounds,
        float zoom, int viewportTopDocumentY) {
        int layoutSignature = Objects.hash(
            page,
            page != null ? page.titleHeading() : null,
            document,
            bounds.x(),
            bounds.y(),
            bounds.width(),
            bounds.height(),
            bounds.contentHeight(),
            bounds.viewportHeight(),
            bounds.maxScroll(),
            viewportTopDocumentY,
            Float.floatToIntBits(zoom));
        if (layoutSignature == lastLayoutSignature) {
            return;
        }
        anchors = collectAnchors(page, document);
        entries = mapAnchorsToEntries(anchors, bounds);
        lastLayoutSignature = layoutSignature;
        if (hoverState != null) {
            if (hoverState.index() < 0 || hoverState.index() >= entries.size()) {
                hoverState = null;
            } else {
                var previous = hoverState;
                hoverState = new HoverState(previous.index(), previous.startedAtMillis());
            }
        }
    }

    private List<OutlineAnchor> collectAnchors(@Nullable GuidePage page, @Nullable LytDocument document) {
        if (document == null || !document.hasLayout()) {
            return List.of();
        }
        List<OutlineAnchor> collected = new ArrayList<>();
        for (var block : document.getBlocks()) {
            if (block instanceof LytHeading heading && heading.getBounds() != null) {
                int depth = heading.getDepth();
                if (depth >= 1 && depth <= 2) {
                    String headingText = heading.getTextContent();
                    if (!headingText.isEmpty()) {
                        collected.add(
                            new OutlineAnchor(
                                headingText,
                                depth,
                                heading.getBounds()
                                    .y(),
                                resolveHeadingColor(depth)));
                    }
                }
            }
        }
        collected.sort(
            Comparator.comparingInt(OutlineAnchor::documentY)
                .thenComparingInt(OutlineAnchor::depth));
        return collected;
    }

    private List<HeadingEntry> mapAnchorsToEntries(List<OutlineAnchor> anchorEntries, ScrollbarBounds bounds) {
        if (anchorEntries.isEmpty() || bounds.height() <= 0) {
            return List.of();
        }
        int track = Math.max(1, bounds.height() - MARKER_HEIGHT);
        int barRight = bounds.x() + bounds.width();
        List<HeadingEntry> mapped = new ArrayList<>(anchorEntries.size());
        for (var anchor : anchorEntries) {
            int markerWidth = markerWidth(anchor.depth());
            int clampedDocumentY = Math.max(0, Math.min(anchor.documentY(), bounds.maxScroll()));
            int markerY = bounds.y()
                + (bounds.maxScroll() > 0 ? (int) ((long) track * clampedDocumentY / bounds.maxScroll()) : 0);
            int markerX = barRight - markerWidth - 1;
            mapped.add(
                new HeadingEntry(
                    anchor.text(),
                    anchor.depth(),
                    anchor.documentY(),
                    anchor.colorArgb(),
                    markerX,
                    markerY,
                    markerWidth,
                    MARKER_HEIGHT,
                    markerX - HIT_PADDING_X,
                    markerY - HIT_PADDING_Y,
                    markerWidth + HIT_PADDING_X * 2,
                    MARKER_HEIGHT + HIT_PADDING_Y * 2));
        }
        return mapped;
    }

    private void updateHoverCandidate(int mouseX, int mouseY, long nowMillis) {
        Integer targetIndex = nearestHoveredIndex(mouseX, mouseY);
        if (targetIndex == null) {
            hoverState = null;
            return;
        }
        if (hoverState != null && hoverState.index() == targetIndex) {
            return;
        }
        hoverState = new HoverState(targetIndex, nowMillis);
    }

    private @Nullable Integer nearestHoveredIndex(int mouseX, int mouseY) {
        Integer nearestIndex = null;
        int nearestDistance = Integer.MAX_VALUE;
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            if (!contains(entry, mouseX, mouseY)) {
                continue;
            }
            int centerY = entry.hitY() + entry.hitHeight() / 2;
            int distance = Math.abs(mouseY - centerY);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = index;
            }
        }
        return nearestIndex;
    }

    private @Nullable LabelLayout buildLabelLayout(HeadingEntry entry, FontRenderer fontRenderer, long nowMillis,
        int labelMaxWidth, int labelGap, int panelLeft, int panelRight) {
        if (hoverState == null || nowMillis - hoverState.startedAtMillis() < HOVER_DELAY_MILLIS) {
            return null;
        }
        List<String> wrappedLines = GuideTextRenderer
            .listFormattedStringToWidth(fontRenderer, entry.text(), labelMaxWidth);
        if (wrappedLines.isEmpty()) {
            wrappedLines = List.of(entry.text());
        }
        List<String> lines = new ArrayList<>(wrappedLines.subList(0, Math.min(LABEL_MAX_LINES, wrappedLines.size())));
        if (wrappedLines.size() > LABEL_MAX_LINES && !lines.isEmpty()) {
            int lastIndex = lines.size() - 1;
            lines.set(lastIndex, truncateLineWithEllipsis(fontRenderer, lines.get(lastIndex), labelMaxWidth));
        }
        int width = 0;
        for (String line : lines) {
            width = Math.max(width, GuideTextRenderer.getStringWidth(fontRenderer, line));
        }
        width = Math.min(labelMaxWidth, width);
        int height = lines.size() * fontRenderer.FONT_HEIGHT;
        int x = Math.max(panelLeft + 4, entry.markerX() - labelGap - width - 10);
        x = Math.min(x, panelRight - width - 4);
        int y = entry.markerY() - height / 2;
        return new LabelLayout(List.copyOf(lines), x, y, width, height, 255);
    }

    private String truncateLineWithEllipsis(FontRenderer fontRenderer, String line, int maxWidth) {
        if (GuideTextRenderer.getStringWidth(fontRenderer, line) <= maxWidth) {
            return line;
        }
        int ellipsisWidth = GuideTextRenderer.getStringWidth(fontRenderer, ELLIPSIS);
        String trimmed = line;
        while (!trimmed.isEmpty()
            && GuideTextRenderer.getStringWidth(fontRenderer, trimmed) + ellipsisWidth > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed + ELLIPSIS;
    }

    private boolean contains(HeadingEntry entry, int mouseX, int mouseY) {
        return mouseX >= entry.hitX() && mouseX < entry.hitX() + entry.hitWidth()
            && mouseY >= entry.hitY()
            && mouseY < entry.hitY() + entry.hitHeight();
    }

    private int resolveHeadingColor(int depth) {
        ResolvedTextStyle baseStyle = DefaultStyles.BASE_STYLE;
        ResolvedTextStyle headingStyle = switch (depth) {
            case 1 -> DefaultStyles.HEADING1.mergeWith(baseStyle);
            case 2 -> DefaultStyles.HEADING2.mergeWith(baseStyle);
            default -> baseStyle;
        };
        ColorValue colorValue = headingStyle.color() != null ? headingStyle.color() : baseStyle.color();
        return colorValue.resolve(LightDarkMode.LIGHT_MODE);
    }

    private static int markerWidth(int depth) {
        return switch (depth) {
            case 1 -> H1_MARKER_WIDTH;
            default -> H2_MARKER_WIDTH;
        };
    }

    public record OutlineAnchor(String text, int depth, int documentY, int colorArgb) {}

    record HoverState(int index, long startedAtMillis) {}
}
