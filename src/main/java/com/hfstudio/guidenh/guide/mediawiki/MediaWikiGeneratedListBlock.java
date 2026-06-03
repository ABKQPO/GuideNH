package com.hfstudio.guidenh.guide.mediawiki;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.GuidePageIcon;
import com.hfstudio.guidenh.guide.PageAnchor;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.DefaultStyles;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.block.BorderRenderer;
import com.hfstudio.guidenh.guide.document.block.LytBlock;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class MediaWikiGeneratedListBlock extends LytBlock implements InteractiveElement {

    private static final int TOP_PADDING = 6;
    private static final int BOTTOM_PADDING = 6;
    private static final int SIDE_PADDING = 2;
    private static final int COLUMN_GAP = 10;
    private static final int ROW_HEIGHT = 20;
    private static final int HEADER_HEIGHT = 20;
    private static final int SECTION_GAP_TOP = 5;
    private static final int SECTION_GAP_BOTTOM = 3;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int LIST_MARKER_SIZE = 3;
    private static final int LIST_MARKER_GAP = 6;
    private static final ConstantColor LIST_MARKER_COLOR = ConstantColor.WHITE;
    private static final ResolvedTextStyle LINK_STYLE = TextStyle.builder()
        .apply(DefaultStyles.BODY_TEXT)
        .color(SymbolicColor.LINK)
        .build()
        .mergeWith(DefaultStyles.BASE_STYLE);
    private static final ResolvedTextStyle HOVER_LINK_STYLE = TextStyle.builder()
        .apply(DefaultStyles.BODY_TEXT)
        .color(SymbolicColor.LINK)
        .underlined(true)
        .build()
        .mergeWith(DefaultStyles.BASE_STYLE);
    private static final ResolvedTextStyle HEADER_STYLE = TextStyle.builder()
        .apply(DefaultStyles.HEADING1)
        .build()
        .mergeWith(DefaultStyles.BASE_STYLE);
    private static final ResolvedTextStyle EMPTY_STYLE = DefaultStyles.BODY_TEXT.mergeWith(DefaultStyles.BASE_STYLE);
    private static final BorderRenderer BORDER_RENDERER = new BorderRenderer();

    private final List<MediaWikiListEntry> entries = new ArrayList<>();
    private final List<RowLayout> rowLayouts = new ArrayList<>();
    private int rows = MediaWikiListPlanner.DEFAULT_ROWS;
    private String emptyText = GuidebookText.MediaWikiNoPages.text();
    @Nullable
    private RowLayout hoveredRow;

    public void setEntries(List<MediaWikiListEntry> entries) {
        this.entries.clear();
        if (entries != null) {
            this.entries.addAll(entries);
        }
    }

    public void setRows(int rows) {
        this.rows = MediaWikiListPlanner.sanitizeRows(rows);
    }

    public void setEmptyText(String emptyText) {
        this.emptyText = emptyText != null && !emptyText.isEmpty() ? emptyText : GuidebookText.MediaWikiNoPages.text();
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        rowLayouts.clear();
        hoveredRow = null;

        int columnCount = Math.max(1, rows);
        int innerWidth = Math.max(0, availableWidth - SIDE_PADDING * 2);
        int columnWidth = Math.max(1, (innerWidth - COLUMN_GAP * (columnCount - 1)) / columnCount);

        if (entries.isEmpty()) {
            rowLayouts
                .add(new RowLayout(new LytRect(x + SIDE_PADDING, y + TOP_PADDING, innerWidth, ROW_HEIGHT), null, null));
            return new LytRect(x, y, availableWidth, TOP_PADDING + ROW_HEIGHT + BOTTOM_PADDING);
        }

        List<MediaWikiListPlanner.MediaWikiListColumn> columns = MediaWikiListPlanner.planColumns(entries, columnCount);
        int maxColumnHeight = 0;
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            int columnX = x + SIDE_PADDING + columnIndex * (columnWidth + COLUMN_GAP);
            int columnY = y + TOP_PADDING;
            for (MediaWikiListPlanner.MediaWikiListSection section : columns.get(columnIndex)
                .sections()) {
                if (!section.entries()
                    .isEmpty()) {
                    columnY += SECTION_GAP_TOP;
                }
                rowLayouts
                    .add(new RowLayout(new LytRect(columnX, columnY, columnWidth, HEADER_HEIGHT), null, section.key()));
                columnY += HEADER_HEIGHT + SECTION_GAP_BOTTOM;
                for (MediaWikiListEntry entry : section.entries()) {
                    rowLayouts.add(new RowLayout(new LytRect(columnX, columnY, columnWidth, ROW_HEIGHT), entry, null));
                    columnY += ROW_HEIGHT;
                }
            }
            maxColumnHeight = Math.max(maxColumnHeight, columnY - y - TOP_PADDING);
        }
        return new LytRect(x, y, availableWidth, TOP_PADDING + maxColumnHeight + BOTTOM_PADDING);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        rowLayouts.replaceAll(layout -> layout.move(deltaX, deltaY));
    }

    @Override
    public void render(RenderContext context) {
        renderBorders(context);
        for (RowLayout rowLayout : rowLayouts) {
            if (!context.intersectsViewport(rowLayout.bounds())) {
                continue;
            }

            MediaWikiListEntry entry = rowLayout.entry();
            if (entry == null && rowLayout.header() == null) {
                rowLayout.setClickableBounds(LytRect.empty());
                int emptyTextY = rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        context,
                        EMPTY_STYLE,
                        rowLayout.bounds()
                            .height());
                context.drawText(
                    emptyText,
                    rowLayout.bounds()
                        .x(),
                    emptyTextY,
                    EMPTY_STYLE);
                continue;
            }

            if (rowLayout.header() != null) {
                int headerTextY = rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        context,
                        HEADER_STYLE,
                        rowLayout.bounds()
                            .height());
                context.drawText(
                    rowLayout.header(),
                    rowLayout.bounds()
                        .x(),
                    headerTextY,
                    HEADER_STYLE);
                rowLayout.setClickableBounds(LytRect.empty());
                continue;
            }

            ResolvedTextStyle rowStyle = rowLayout == hoveredRow ? HOVER_LINK_STYLE : LINK_STYLE;
            int rowTextY = rowLayout.bounds()
                .y()
                + verticalCenterOffset(
                    context,
                    rowStyle,
                    rowLayout.bounds()
                        .height());
            int markerX = rowLayout.bounds()
                .x();
            int markerY = rowLayout.bounds()
                .y()
                + (rowLayout.bounds()
                    .height() - LIST_MARKER_SIZE) / 2;
            context.fillRect(markerX, markerY, LIST_MARKER_SIZE, LIST_MARKER_SIZE, LIST_MARKER_COLOR);
            int textX = markerX + LIST_MARKER_SIZE + LIST_MARKER_GAP;
            GuidePageIcon icon = entry.icon();
            if (icon != null) {
                renderIcon(
                    context,
                    icon,
                    textX,
                    rowLayout.bounds()
                        .y()
                        + (rowLayout.bounds()
                            .height() - ICON_SIZE) / 2);
                textX += ICON_SIZE + ICON_GAP;
            }

            String renderedTitle = clipToWidth(
                context,
                entry.title(),
                Math.max(
                    1,
                    rowLayout.bounds()
                        .right() - textX));
            int clickableWidth = textX - rowLayout.bounds()
                .x();
            if (renderedTitle != null && !renderedTitle.isEmpty()) {
                clickableWidth += context.getStringWidth(renderedTitle, rowStyle);
            }
            rowLayout.setClickableBounds(
                new LytRect(
                    rowLayout.bounds()
                        .x(),
                    rowLayout.bounds()
                        .y(),
                    Math.max(0, clickableWidth),
                    rowLayout.bounds()
                        .height()));
            context.drawText(renderedTitle, textX, rowTextY, rowStyle);
        }
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0) {
            return false;
        }

        for (RowLayout rowLayout : rowLayouts) {
            if (!rowLayout.clickableBounds()
                .contains(x, y)) {
                continue;
            }
            MediaWikiListEntry entry = rowLayout.entry();
            if (entry == null) {
                return false;
            }
            screen.navigateTo(PageAnchor.page(entry.pageId()));
            return true;
        }
        return false;
    }

    @Override
    public void onMouseEnter(@Nullable LytFlowContent hoveredContent) {
        hoveredRow = null;
    }

    @Override
    public void onMouseLeave() {
        hoveredRow = null;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        hoveredRow = findClickableRow((int) x, (int) y);
        return Optional.empty();
    }

    @Nullable
    private RowLayout findClickableRow(int x, int y) {
        for (RowLayout rowLayout : rowLayouts) {
            if (rowLayout.entry() != null && rowLayout.clickableBounds()
                .contains(x, y)) {
                return rowLayout;
            }
        }
        return null;
    }

    private void renderIcon(RenderContext context, GuidePageIcon icon, int x, int y) {
        if (icon.isItemIcon() && icon.resolveCurrentItemStack() != null) {
            context.renderItemIcon(icon.resolveCurrentItemStack(), x, y);
            return;
        }
        if (icon.resolveCurrentTexture() != null) {
            context.fillTexturedRect(new LytRect(x, y, ICON_SIZE, ICON_SIZE), icon.resolveCurrentTexture());
        }
    }

    private int verticalCenterOffset(RenderContext context, ResolvedTextStyle style, int boxHeight) {
        return Math.max(0, (boxHeight - context.getLineHeight(style)) / 2);
    }

    private void renderBorders(RenderContext context) {
        if (getBorderTop().width() <= 0 && getBorderLeft().width() <= 0
            && getBorderRight().width() <= 0
            && getBorderBottom().width() <= 0) {
            return;
        }
        BORDER_RENDERER.render(context, bounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
    }

    private String clipToWidth(RenderContext context, String text, int maxWidth) {
        if (text == null || text.isEmpty() || context.getStringWidth(text, LINK_STYLE) <= maxWidth) {
            return text == null ? "" : text;
        }

        int ellipsisWidth = context.getStringWidth("...", LINK_STYLE);
        if (ellipsisWidth >= maxWidth) {
            return "";
        }

        int end = text.length();
        while (end > 0 && context.getStringWidth(text.substring(0, end), LINK_STYLE) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? "..." : text.substring(0, end) + "...";
    }

    private static class RowLayout {

        private final LytRect bounds;
        @Nullable
        private final MediaWikiListEntry entry;
        @Nullable
        private final String header;
        private LytRect clickableBounds;

        private RowLayout(LytRect bounds, @Nullable MediaWikiListEntry entry, @Nullable String header) {
            this.bounds = bounds;
            this.entry = entry;
            this.header = header;
            this.clickableBounds = LytRect.empty();
        }

        private LytRect bounds() {
            return bounds;
        }

        @Nullable
        private MediaWikiListEntry entry() {
            return entry;
        }

        @Nullable
        private String header() {
            return header;
        }

        private LytRect clickableBounds() {
            return clickableBounds;
        }

        private void setClickableBounds(LytRect clickableBounds) {
            this.clickableBounds = clickableBounds != null ? clickableBounds : LytRect.empty();
        }

        private RowLayout move(int deltaX, int deltaY) {
            RowLayout moved = new RowLayout(bounds.move(deltaX, deltaY), entry, header);
            moved.setClickableBounds(clickableBounds.move(deltaX, deltaY));
            return moved;
        }
    }
}
