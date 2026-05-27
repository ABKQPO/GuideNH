package com.hfstudio.guidenh.guide.mediawiki;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
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
import com.hfstudio.guidenh.guide.document.interaction.TextTooltip;
import com.hfstudio.guidenh.guide.internal.GuidebookText;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class MediaWikiSpecialGeneratedBlock extends LytBlock implements InteractiveElement {

    private static final int SPECIAL_PAGES_GROUP_COLUMNS = 2;
    private static final int TOP_PADDING = 6;
    private static final int BOTTOM_PADDING = 6;
    private static final int SIDE_PADDING = 2;
    private static final int COLUMN_GAP = 10;
    private static final int GROUP_MARGIN = 6;
    private static final int HEADER_HEIGHT = 20;
    private static final int ENTRY_HEIGHT = 20;
    private static final int ENTRY_GAP = 2;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 4;
    private static final int LIST_MARKER_SIZE = 3;
    private static final int LIST_MARKER_GAP = 6;
    private static final int TITLE_SUBTITLE_GAP = 3;
    private static final int ENTRY_VERTICAL_PADDING_TOP = 3;
    private static final int ENTRY_VERTICAL_PADDING_BOTTOM = 3;
    private static final int HEADER_MARGIN_TOP = 5;
    private static final int HEADER_MARGIN_BOTTOM = 5;
    private static final int LOAD_MORE_HEIGHT = 18;
    private static final int LOAD_MORE_MARGIN_TOP = 2;
    private static final String ELLIPSIS = "...";
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
    private static final ResolvedTextStyle SUBTITLE_STYLE = TextStyle.builder()
        .apply(DefaultStyles.BODY_TEXT)
        .color(SymbolicColor.GRAY)
        .build()
        .mergeWith(DefaultStyles.BASE_STYLE);
    private static final ResolvedTextStyle EMPTY_STYLE = DefaultStyles.BODY_TEXT.mergeWith(DefaultStyles.BASE_STYLE);
    private static final BorderRenderer BORDER_RENDERER = new BorderRenderer();

    private final List<RowLayout> rowLayouts = new ArrayList<>();
    private MediaWikiSpecialPageResult result = MediaWikiSpecialPageModels.info(
        new MediaWikiSpecialDefinition(
            "Special",
            "guidenh.mediawiki.special.unsupported",
            "other",
            MediaWikiSpecialPageKind.INFO,
            false,
            false,
            MediaWikiSpecialPageQuery.PAGE_SIZE,
            null),
        "");
    private String searchQuery = "";
    private int rows = MediaWikiListPlanner.DEFAULT_ROWS;
    private String emptyText = GuidebookText.MediaWikiNoPages.text();
    private int currentVisibleCount = MediaWikiSpecialPageQuery.PAGE_SIZE;
    @Nullable
    private VisibilityCache visibilityCache;
    @Nullable
    private RowLayout hoveredRow;
    @Nullable
    private MediaWikiListContext listContext;
    @Nullable
    private MediaWikiSpecialDefinition definition;
    @Nullable
    private MediaWikiSpecialPageResolver resolver;
    @Nullable
    private Map<String, String> queryParameters;

    public void setResult(MediaWikiSpecialPageResult result) {
        if (result != null) {
            this.result = result;
            definition = result.definition();
            visibilityCache = null;
            currentVisibleCount = resolveDefaultVisibleCount(result);
        }
    }

    public void setResolverContext(MediaWikiListContext listContext, MediaWikiSpecialDefinition definition,
        MediaWikiSpecialPageResolver resolver, @Nullable Map<String, String> queryParameters) {
        this.listContext = listContext;
        this.definition = definition;
        this.resolver = resolver;
        this.queryParameters = queryParameters != null && !queryParameters.isEmpty()
            ? new LinkedHashMap<>(queryParameters)
            : Collections.<String, String>emptyMap();
    }

    public void setRows(int rows) {
        this.rows = Math.max(1, rows);
    }

    public void setEmptyText(String emptyText) {
        this.emptyText = emptyText != null && !emptyText.isEmpty() ? emptyText : GuidebookText.MediaWikiNoPages.text();
    }

    public void setSearchQuery(String searchQuery) {
        String nextSearchQuery = searchQuery != null ? searchQuery : "";
        if (this.searchQuery.equals(nextSearchQuery)) {
            return;
        }
        this.searchQuery = nextSearchQuery;
        if (supportsResolverBackedSearch()) {
            refreshResolverBackedResult(nextSearchQuery);
        }
        if (MediaWikiSpecialSearchSupport.normalize(nextSearchQuery)
            .isEmpty()) {
            currentVisibleCount = resolveDefaultVisibleCount(result);
        }
        visibilityCache = null;
        var document = getDocument();
        if (document != null) {
            document.invalidateLayout();
        }
    }

    private boolean supportsResolverBackedSearch() {
        return listContext != null && definition != null
            && resolver != null
            && definition.name() != null
            && (MediaWikiSpecialPageIds.PAGE_TRANSLATION.equals(definition.name())
                || MediaWikiSpecialPageIds.SEARCH_TRANSLATIONS.equals(definition.name())
                || MediaWikiSpecialPageIds.ALL_TRANSLATIONS.equals(definition.name())
                || MediaWikiSpecialPageIds.ALL_PAGES_WITH_PREFIX.equals(definition.name()));
    }

    private void refreshResolverBackedResult(String queryText) {
        if (!supportsResolverBackedSearch()) {
            return;
        }
        MediaWikiSpecialPageQuery query = new MediaWikiSpecialPageQuery(
            queryText != null ? queryText : "",
            Integer.MAX_VALUE,
            queryParameters != null ? queryParameters : Collections.<String, String>emptyMap());
        MediaWikiSpecialPageResult refreshed = resolver.resolve(listContext, definition, query);
        if (refreshed != null) {
            result = refreshed;
            definition = refreshed.definition();
        }
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        rowLayouts.clear();
        hoveredRow = null;

        MediaWikiSpecialPageResult visibleResult = applyVisibility(result, searchQuery);
        int columnCount = resolveColumnCount(visibleResult);
        int innerWidth = Math.max(0, availableWidth - SIDE_PADDING * 2);
        int columnWidth = Math.max(1, (innerWidth - COLUMN_GAP * (columnCount - 1)) / columnCount);

        if (isEmpty(visibleResult)) {
            rowLayouts
                .add(new RowLayout(new LytRect(x + SIDE_PADDING, y + TOP_PADDING, innerWidth, ENTRY_HEIGHT), null));
            return new LytRect(x, y, availableWidth, TOP_PADDING + ENTRY_HEIGHT + BOTTOM_PADDING);
        }

        List<List<GroupLayout>> columns = layoutColumns(visibleResult);
        int maxColumnHeight = 0;
        for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
            int columnX = x + SIDE_PADDING + columnIndex * (columnWidth + COLUMN_GAP);
            int columnY = y + TOP_PADDING;
            for (GroupLayout group : columns.get(columnIndex)) {
                if (group.title() != null) {
                    rowLayouts.add(
                        new RowLayout(
                            new LytRect(columnX, columnY + HEADER_MARGIN_TOP, columnWidth, HEADER_HEIGHT),
                            new RenderRow(group.title(), "", null, null, null, null, true, false)));
                    columnY += HEADER_MARGIN_TOP + HEADER_HEIGHT + HEADER_MARGIN_BOTTOM;
                }
                for (MediaWikiSpecialListEntry entry : group.entries()) {
                    int entryHeight = computeEntryHeight(context, entry, columnWidth);
                    rowLayouts.add(
                        new RowLayout(
                            new LytRect(columnX, columnY, columnWidth, entryHeight),
                            new RenderRow(
                                entry.title(),
                                entry.subtitle(),
                                entry.pageId(),
                                entry.lineNumber(),
                                entry.icon(),
                                entry.externalUrl(),
                                false,
                                false)));
                    columnY += entryHeight + ENTRY_GAP;
                }
                columnY += GROUP_MARGIN;
            }
            maxColumnHeight = Math.max(maxColumnHeight, columnY - y - TOP_PADDING);
        }
        if (visibleResult.hasMore()) {
            rowLayouts.add(
                new RowLayout(
                    new LytRect(
                        x + SIDE_PADDING,
                        y + TOP_PADDING + maxColumnHeight + LOAD_MORE_MARGIN_TOP,
                        innerWidth,
                        LOAD_MORE_HEIGHT),
                    RenderRow.loadMore()));
            maxColumnHeight += LOAD_MORE_MARGIN_TOP + LOAD_MORE_HEIGHT;
        }
        return new LytRect(x, y, availableWidth, TOP_PADDING + maxColumnHeight + BOTTOM_PADDING);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        for (int index = 0; index < rowLayouts.size(); index++) {
            rowLayouts.set(
                index,
                rowLayouts.get(index)
                    .move(deltaX, deltaY));
        }
    }

    @Override
    public void render(RenderContext context) {
        renderBorders(context);
        for (RowLayout rowLayout : rowLayouts) {
            if (!context.intersectsViewport(rowLayout.bounds())) {
                continue;
            }

            RenderRow row = rowLayout.row();
            if (row == null) {
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

            if (row.header()) {
                rowLayout.setClickableBounds(LytRect.empty());
                String renderedHeader = clipToWidth(
                    context,
                    row.title(),
                    rowLayout.bounds()
                        .width(),
                    HEADER_STYLE);
                int headerTextY = rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        context,
                        HEADER_STYLE,
                        rowLayout.bounds()
                            .height());
                context.drawText(
                    renderedHeader,
                    rowLayout.bounds()
                        .x(),
                    headerTextY,
                    HEADER_STYLE);
                rowLayout.setTooltip(isClipped(renderedHeader, row.title()) ? new TextTooltip(row.title()) : null);
                continue;
            }

            if (row.loadMoreRow()) {
                ResolvedTextStyle rowStyle = rowLayout == hoveredRow ? HOVER_LINK_STYLE : LINK_STYLE;
                int loadMoreY = rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        context,
                        rowStyle,
                        rowLayout.bounds()
                            .height());
                context.drawText(
                    GuidebookText.SpecialPageShowMore.text(),
                    rowLayout.bounds()
                        .x(),
                    loadMoreY,
                    rowStyle);
                rowLayout.setClickableBounds(rowLayout.bounds());
                continue;
            }

            ResolvedTextStyle rowStyle = rowLayout == hoveredRow ? HOVER_LINK_STYLE : LINK_STYLE;
            int textMaxWidth = Math.max(
                1,
                rowLayout.bounds()
                    .right()
                    - rowLayout.bounds()
                        .x()
                    - LIST_MARKER_SIZE
                    - LIST_MARKER_GAP
                    - (row.icon() != null ? ICON_SIZE + ICON_GAP : 0));
            List<String> subtitleLines = wrapLines(context, row.subtitle(), textMaxWidth, SUBTITLE_STYLE);
            int markerX = rowLayout.bounds()
                .x();
            int contentTop = rowLayout.bounds()
                .y() + rowContentTop(context, rowStyle, subtitleLines);
            int contentHeight = rowContentHeight(context, rowStyle, subtitleLines);
            int markerY = contentTop + Math.max(0, (contentHeight - LIST_MARKER_SIZE) / 2);
            context.fillRect(markerX, markerY, LIST_MARKER_SIZE, LIST_MARKER_SIZE, LIST_MARKER_COLOR);

            int textX = markerX + LIST_MARKER_SIZE + LIST_MARKER_GAP;
            if (row.icon() != null) {
                renderIcon(context, row.icon(), textX, contentTop + Math.max(0, (contentHeight - ICON_SIZE) / 2));
                textX += ICON_SIZE + ICON_GAP;
            }
            String renderedTitle = clipToWidth(
                context,
                row.title(),
                Math.max(
                    1,
                    rowLayout.bounds()
                        .right() - textX),
                rowStyle);
            String fullTitle = row.title() != null ? row.title() : "";
            boolean titleClipped = isClipped(renderedTitle, fullTitle);
            String fullSubtitle = row.subtitle() != null ? row.subtitle() : "";
            boolean hasSubtitle = !fullSubtitle.isEmpty();
            int titleY = hasSubtitle ? contentTop
                : rowLayout.bounds()
                    .y()
                    + verticalCenterOffset(
                        context,
                        rowStyle,
                        rowLayout.bounds()
                            .height());
            context.drawText(renderedTitle, textX, titleY, rowStyle);

            int clickableWidth = textX - rowLayout.bounds()
                .x();
            if (renderedTitle != null && !renderedTitle.isEmpty()) {
                clickableWidth += context.getStringWidth(renderedTitle, rowStyle);
            }
            int clickableHeight = context.getLineHeight(rowStyle);

            if (row.subtitle() != null && !row.subtitle()
                .isEmpty()) {
                boolean subtitleClipped = areLinesClipped(subtitleLines, fullSubtitle);
                int subtitleY = contentTop + context.getLineHeight(rowStyle) + TITLE_SUBTITLE_GAP;
                for (String subtitleLine : subtitleLines) {
                    context.drawText(subtitleLine, textX, subtitleY, SUBTITLE_STYLE);
                    clickableWidth = Math.max(
                        clickableWidth,
                        textX - rowLayout.bounds()
                            .x() + context.getStringWidth(subtitleLine, SUBTITLE_STYLE));
                    subtitleY += context.getLineHeight(SUBTITLE_STYLE);
                }
                clickableWidth = Math.max(
                    clickableWidth,
                    textX - rowLayout.bounds()
                        .x());
                clickableHeight = rowContentHeight(context, rowStyle, subtitleLines);
                rowLayout.setTooltip(
                    titleClipped || subtitleClipped ? new TextTooltip(fullTitle + "\n" + fullSubtitle)
                        : titleClipped ? new TextTooltip(fullTitle)
                            : subtitleClipped ? new TextTooltip(fullSubtitle) : null);
            } else {
                rowLayout.setTooltip(titleClipped ? new TextTooltip(fullTitle) : null);
            }

            rowLayout.setClickableBounds(
                row.pageId() != null || row.externalUrl() != null ? new LytRect(
                    rowLayout.bounds()
                        .x(),
                    contentTop,
                    Math.max(
                        LIST_MARKER_SIZE + LIST_MARKER_GAP,
                        Math.min(
                            rowLayout.bounds()
                                .width(),
                            clickableWidth)),
                    Math.max(ICON_SIZE, clickableHeight)) : LytRect.empty());
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
            RenderRow row = rowLayout.row();
            if (row == null) {
                return false;
            }
            if (row.pageId() != null) {
                String rowAnchor = row.lineNumber() != null ? "line-" + row.lineNumber() : null;
                screen.navigateTo(new PageAnchor(row.pageId(), rowAnchor));
                return true;
            }
            if (row.loadMoreRow()) {
                loadMore();
                return true;
            }
            URI externalUri = MediaWikiExternalLinkSupport.resolveExternalUri(row.externalUrl());
            if (externalUri != null) {
                screen.openExternalUrl(externalUri);
                return true;
            }
            return false;
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
        hoveredRow = findTooltipRow((int) x, (int) y);
        return hoveredRow != null ? Optional.ofNullable(hoveredRow.tooltip()) : Optional.empty();
    }

    private int resolveColumnCount(MediaWikiSpecialPageResult visibleResult) {
        if (visibleResult.kind() == MediaWikiSpecialPageKind.GROUP_INDEX && visibleResult.definition() != null
            && MediaWikiSpecialPageIds.SPECIAL_PAGES.equals(
                visibleResult.definition()
                    .name())) {
            return SPECIAL_PAGES_GROUP_COLUMNS;
        }
        if (visibleResult.definition() != null && (MediaWikiSpecialPageIds.ALL_TRANSLATIONS.equals(
            visibleResult.definition()
                .name())
            || MediaWikiSpecialPageIds.PAGE_TRANSLATION.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.SEARCH_TRANSLATIONS.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.GLOBAL_FILE_USAGE.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.DOUBLE_REDIRECTS.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.LINT_ERRORS.equals(
                visibleResult.definition()
                    .name())
            || MediaWikiSpecialPageIds.OVERRIDDEN_PAGES.equals(
                visibleResult.definition()
                    .name()))) {
            return 1;
        }
        if (visibleResult.definition() != null && MediaWikiSpecialPageIds.CATEGORY_TREE.equals(
            visibleResult.definition()
                .name())) {
            return 2;
        }
        if (visibleResult.kind() == MediaWikiSpecialPageKind.GROUPED
            || visibleResult.kind() == MediaWikiSpecialPageKind.GROUP_INDEX) {
            return Math.max(1, rows);
        }
        return Math.max(1, rows);
    }

    private boolean isEmpty(MediaWikiSpecialPageResult visibleResult) {
        if (visibleResult == null) {
            return true;
        }
        return visibleResult.kind() == MediaWikiSpecialPageKind.GROUPED
            || visibleResult.kind() == MediaWikiSpecialPageKind.GROUP_INDEX
                ? visibleResult.groupedEntries()
                    .isEmpty()
                : visibleResult.flatEntries()
                    .isEmpty();
    }

    private List<List<GroupLayout>> layoutColumns(MediaWikiSpecialPageResult visibleResult) {
        int columnCount = resolveColumnCount(visibleResult);
        if (visibleResult.kind() != MediaWikiSpecialPageKind.GROUPED
            && visibleResult.kind() != MediaWikiSpecialPageKind.GROUP_INDEX) {
            return layoutFlatColumns(visibleResult, columnCount);
        }
        ArrayList<List<GroupLayout>> columns = new ArrayList<>(columnCount);
        ArrayList<Integer> heights = new ArrayList<>(columnCount);
        for (int index = 0; index < columnCount; index++) {
            columns.add(new ArrayList<>());
            heights.add(0);
        }

        for (GroupLayout group : buildGroups(visibleResult)) {
            int targetColumn = 0;
            for (int index = 1; index < columnCount; index++) {
                if (heights.get(index) < heights.get(targetColumn)) {
                    targetColumn = index;
                }
            }
            columns.get(targetColumn)
                .add(group);
            heights.set(targetColumn, heights.get(targetColumn) + estimateHeight(group));
        }

        return columns;
    }

    private List<List<GroupLayout>> layoutFlatColumns(MediaWikiSpecialPageResult visibleResult, int columnCount) {
        ArrayList<List<GroupLayout>> columns = new ArrayList<>(columnCount);
        for (int index = 0; index < columnCount; index++) {
            columns.add(new ArrayList<GroupLayout>());
        }
        List<MediaWikiSpecialListEntry> entries = visibleResult.flatEntries() != null ? visibleResult.flatEntries()
            : Collections.<MediaWikiSpecialListEntry>emptyList();
        if (entries.isEmpty()) {
            columns.get(0)
                .add(new GroupLayout(null, Collections.<MediaWikiSpecialListEntry>emptyList()));
            return columns;
        }
        int perColumn = Math.max(1, (entries.size() + columnCount - 1) / columnCount);
        for (int columnIndex = 0; columnIndex < columnCount; columnIndex++) {
            int startIndex = columnIndex * perColumn;
            if (startIndex >= entries.size()) {
                break;
            }
            int endIndex = Math.min(entries.size(), startIndex + perColumn);
            columns.get(columnIndex)
                .add(new GroupLayout(null, new ArrayList<>(entries.subList(startIndex, endIndex))));
        }
        return columns;
    }

    private List<GroupLayout> buildGroups(MediaWikiSpecialPageResult visibleResult) {
        ArrayList<GroupLayout> groups = new ArrayList<>();
        if (visibleResult.kind() == MediaWikiSpecialPageKind.GROUPED
            || visibleResult.kind() == MediaWikiSpecialPageKind.GROUP_INDEX) {
            for (MediaWikiSpecialGroupedEntry group : visibleResult.groupedEntries()) {
                groups.add(
                    new GroupLayout(
                        group.title(),
                        group.children() != null ? group.children() : Collections.emptyList()));
            }
            return groups;
        }
        groups.add(
            new GroupLayout(
                null,
                visibleResult.flatEntries() != null ? visibleResult.flatEntries() : Collections.emptyList()));
        return groups;
    }

    private MediaWikiSpecialPageResult applyVisibility(MediaWikiSpecialPageResult source, String rawQuery) {
        if (source == null) {
            return result;
        }
        String normalizedQuery = MediaWikiSpecialSearchSupport.normalize(rawQuery);
        VisibilityCache cached = visibilityCache;
        if (cached != null && cached.matches(source, normalizedQuery, currentVisibleCount)) {
            return cached.result();
        }
        MediaWikiSpecialPageResult computed = computeVisibility(source, normalizedQuery);
        visibilityCache = new VisibilityCache(source, normalizedQuery, currentVisibleCount, computed);
        return computed;
    }

    private MediaWikiSpecialPageResult computeVisibility(MediaWikiSpecialPageResult source, String normalizedQuery) {
        MediaWikiSpecialPageResult filtered = source;
        if (!normalizedQuery.isEmpty()) {
            if (source.kind() == MediaWikiSpecialPageKind.GROUPED
                || source.kind() == MediaWikiSpecialPageKind.GROUP_INDEX) {
                filtered = new MediaWikiSpecialPageResult(
                    source.definition(),
                    source.kind(),
                    source.flatEntries(),
                    MediaWikiSpecialSearchSupport.filterGroupedEntries(source.groupedEntries(), normalizedQuery),
                    false,
                    source.searchEnabled());
            } else {
                filtered = new MediaWikiSpecialPageResult(
                    source.definition(),
                    source.kind(),
                    MediaWikiSpecialSearchSupport.filterFlatEntries(source.flatEntries(), normalizedQuery),
                    source.groupedEntries(),
                    false,
                    source.searchEnabled());
            }
            return filtered;
        }

        if (currentVisibleCount == Integer.MAX_VALUE) {
            return source;
        }

        if (source.kind() == MediaWikiSpecialPageKind.GROUPED
            || source.kind() == MediaWikiSpecialPageKind.GROUP_INDEX) {
            int totalChildren = countGroupedChildren(source.groupedEntries());
            return new MediaWikiSpecialPageResult(
                source.definition(),
                source.kind(),
                source.flatEntries(),
                MediaWikiSpecialSearchSupport.limit(source.groupedEntries(), currentVisibleCount),
                currentVisibleCount < totalChildren,
                source.searchEnabled());
        }
        return new MediaWikiSpecialPageResult(
            source.definition(),
            source.kind(),
            MediaWikiSpecialSearchSupport.limit(source.flatEntries(), currentVisibleCount),
            source.groupedEntries(),
            MediaWikiSpecialSearchSupport.hasMore(source.flatEntries(), currentVisibleCount),
            source.searchEnabled());
    }

    private int resolveDefaultVisibleCount(MediaWikiSpecialPageResult source) {
        if (source == null || source.definition() == null) {
            return Integer.MAX_VALUE;
        }
        return source.definition()
            .showsAllByDefault() ? Integer.MAX_VALUE
                : Math.max(
                    1,
                    source.definition()
                        .defaultVisibleCount());
    }

    private int countGroupedChildren(List<MediaWikiSpecialGroupedEntry> groups) {
        if (groups == null || groups.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (MediaWikiSpecialGroupedEntry group : groups) {
            if (group != null && group.children() != null) {
                total += group.children()
                    .size();
            }
        }
        return total;
    }

    private void loadMore() {
        if (currentVisibleCount == Integer.MAX_VALUE) {
            return;
        }
        currentVisibleCount = Math.min(
            Integer.MAX_VALUE - MediaWikiSpecialPageQuery.PAGE_SIZE,
            currentVisibleCount + MediaWikiSpecialPageQuery.PAGE_SIZE);
        visibilityCache = null;
        var document = getDocument();
        if (document != null) {
            document.invalidateLayout();
        }
    }

    private int estimateHeight(GroupLayout group) {
        int height = group.title() != null ? HEADER_MARGIN_TOP + HEADER_HEIGHT + HEADER_MARGIN_BOTTOM : 0;
        for (MediaWikiSpecialListEntry entry : group.entries()) {
            height += estimateEntryHeight(entry);
            height += ENTRY_GAP;
        }
        return height + GROUP_MARGIN;
    }

    @Nullable
    private RowLayout findClickableRow(int x, int y) {
        for (RowLayout rowLayout : rowLayouts) {
            if (rowLayout.row() != null && rowLayout.clickableBounds()
                .contains(x, y)) {
                return rowLayout;
            }
        }
        return null;
    }

    @Nullable
    private RowLayout findTooltipRow(int x, int y) {
        for (RowLayout rowLayout : rowLayouts) {
            if (rowLayout.tooltip() == null) {
                continue;
            }
            if (rowLayout.bounds()
                .contains(x, y)) {
                return rowLayout;
            }
        }
        return findClickableRow(x, y);
    }

    private int verticalCenterOffset(RenderContext context, ResolvedTextStyle style, int boxHeight) {
        return Math.max(0, (boxHeight - context.getLineHeight(style)) / 2);
    }

    private int rowContentTop(RenderContext context, ResolvedTextStyle style, List<String> subtitleLines) {
        if (subtitleLines.isEmpty()) {
            return 0;
        }
        return ENTRY_VERTICAL_PADDING_TOP;
    }

    private int rowContentHeight(RenderContext context, ResolvedTextStyle style, List<String> subtitleLines) {
        if (subtitleLines.isEmpty()) {
            return Math.max(context.getLineHeight(style), ICON_SIZE);
        }
        return Math.max(
            ICON_SIZE,
            context.getLineHeight(style) + TITLE_SUBTITLE_GAP
                + context.getLineHeight(SUBTITLE_STYLE) * subtitleLines.size());
    }

    private int computeEntryHeight(LayoutContext context, MediaWikiSpecialListEntry entry, int columnWidth) {
        int textMaxWidth = computeTextMaxWidth(columnWidth, entry.icon() != null);
        List<String> subtitleLines = wrapLines(context, entry.subtitle(), textMaxWidth, SUBTITLE_STYLE);
        if (subtitleLines.isEmpty()) {
            return ENTRY_HEIGHT;
        }
        int contentHeight = Math.max(
            ICON_SIZE,
            context.getLineHeight(LINK_STYLE) + TITLE_SUBTITLE_GAP
                + context.getLineHeight(SUBTITLE_STYLE) * subtitleLines.size());
        return Math.max(ENTRY_HEIGHT, ENTRY_VERTICAL_PADDING_TOP + contentHeight + ENTRY_VERTICAL_PADDING_BOTTOM);
    }

    private int estimateEntryHeight(MediaWikiSpecialListEntry entry) {
        if (entry.subtitle() == null || entry.subtitle()
            .isEmpty()) {
            return ENTRY_HEIGHT;
        }
        int lineCount = Math.max(
            1,
            GuideStringLines.splitLines(entry.subtitle())
                .size());
        return Math.max(
            ENTRY_HEIGHT,
            ENTRY_VERTICAL_PADDING_TOP + Math.max(ICON_SIZE, 9 + TITLE_SUBTITLE_GAP + 9 * lineCount)
                + ENTRY_VERTICAL_PADDING_BOTTOM);
    }

    private int computeTextMaxWidth(int columnWidth, boolean hasIcon) {
        return Math.max(1, columnWidth - LIST_MARKER_SIZE - LIST_MARKER_GAP - (hasIcon ? ICON_SIZE + ICON_GAP : 0));
    }

    private List<String> wrapLines(LayoutContext context, @Nullable String text, int maxWidth,
        ResolvedTextStyle style) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> lines = new ArrayList<>();
        for (String rawLine : GuideStringLines.splitLines(text)) {
            appendWrappedLine(context, rawLine, maxWidth, style, lines);
        }
        return lines;
    }

    private List<String> wrapLines(RenderContext context, @Nullable String text, int maxWidth,
        ResolvedTextStyle style) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        ArrayList<String> lines = new ArrayList<>();
        for (String rawLine : GuideStringLines.splitLines(text)) {
            appendWrappedLine(context, rawLine, maxWidth, style, lines);
        }
        return lines;
    }

    private void appendWrappedLine(LayoutContext context, String rawLine, int maxWidth, ResolvedTextStyle style,
        List<String> output) {
        String line = rawLine != null ? rawLine.trim() : "";
        if (line.isEmpty()) {
            return;
        }
        if (measureTextWidth(context, style, line) <= maxWidth) {
            output.add(line);
            return;
        }
        String[] words = line.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isEmpty()) {
                continue;
            }
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (measureTextWidth(context, style, candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (current.length() > 0) {
                output.add(current.toString());
                current.setLength(0);
            }
            appendBrokenWord(context, word, maxWidth, style, output);
        }
        if (current.length() > 0) {
            output.add(current.toString());
        }
    }

    private void appendWrappedLine(RenderContext context, String rawLine, int maxWidth, ResolvedTextStyle style,
        List<String> output) {
        String line = rawLine != null ? rawLine.trim() : "";
        if (line.isEmpty()) {
            return;
        }
        if (context.getStringWidth(line, style) <= maxWidth) {
            output.add(line);
            return;
        }
        String[] words = line.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isEmpty()) {
                continue;
            }
            String candidate = current.length() == 0 ? word : current + " " + word;
            if (context.getStringWidth(candidate, style) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }
            if (current.length() > 0) {
                output.add(current.toString());
                current.setLength(0);
            }
            appendBrokenWord(context, word, maxWidth, style, output);
        }
        if (current.length() > 0) {
            output.add(current.toString());
        }
    }

    private void appendBrokenWord(LayoutContext context, String word, int maxWidth, ResolvedTextStyle style,
        List<String> output) {
        if (measureTextWidth(context, style, word) <= maxWidth) {
            output.add(word);
            return;
        }
        int start = 0;
        while (start < word.length()) {
            int end = start + 1;
            while (end <= word.length() && measureTextWidth(context, style, word.substring(start, end)) <= maxWidth) {
                end++;
            }
            int safeEnd = Math.max(start + 1, end - 1);
            output.add(word.substring(start, safeEnd));
            start = safeEnd;
        }
    }

    private void appendBrokenWord(RenderContext context, String word, int maxWidth, ResolvedTextStyle style,
        List<String> output) {
        if (context.getStringWidth(word, style) <= maxWidth) {
            output.add(word);
            return;
        }
        int start = 0;
        while (start < word.length()) {
            int end = start + 1;
            while (end <= word.length() && context.getStringWidth(word.substring(start, end), style) <= maxWidth) {
                end++;
            }
            int safeEnd = Math.max(start + 1, end - 1);
            output.add(word.substring(start, safeEnd));
            start = safeEnd;
        }
    }

    private boolean areLinesClipped(List<String> renderedLines, String originalText) {
        if (originalText == null || originalText.isEmpty()) {
            return false;
        }
        List<String> originalLines = GuideStringLines.splitLines(originalText);
        if (renderedLines.size() != originalLines.size()) {
            return true;
        }
        for (int index = 0; index < renderedLines.size(); index++) {
            if (!renderedLines.get(index)
                .equals(originalLines.get(index))) {
                return true;
            }
        }
        return false;
    }

    private int measureTextWidth(LayoutContext context, ResolvedTextStyle style, String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0f;
        int offset = 0;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            width += context.getAdvance(codePoint, style);
            offset += Character.charCount(codePoint);
        }
        return Math.round(width);
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

    private void renderBorders(RenderContext context) {
        if (getBorderTop().width() <= 0 && getBorderLeft().width() <= 0
            && getBorderRight().width() <= 0
            && getBorderBottom().width() <= 0) {
            return;
        }
        BORDER_RENDERER.render(context, bounds, getBorderTop(), getBorderLeft(), getBorderRight(), getBorderBottom());
    }

    private String clipToWidth(RenderContext context, String text, int maxWidth, ResolvedTextStyle style) {
        if (text == null || text.isEmpty() || context.getStringWidth(text, style) <= maxWidth) {
            return text == null ? "" : text;
        }

        int ellipsisWidth = context.getStringWidth("...", style);
        if (ellipsisWidth >= maxWidth) {
            return ELLIPSIS;
        }

        int end = text.length();
        while (end > 0 && context.getStringWidth(text.substring(0, end), style) + ellipsisWidth > maxWidth) {
            end--;
        }
        return end <= 0 ? ELLIPSIS : text.substring(0, end) + ELLIPSIS;
    }

    private boolean isClipped(String rendered, String original) {
        if (original == null || original.isEmpty()) {
            return false;
        }
        return rendered != null && rendered.endsWith("...") && !rendered.equals(original);
    }

    @Desugar
    private record GroupLayout(@Nullable String title, List<MediaWikiSpecialListEntry> entries) {}

    @Desugar
    private record RenderRow(String title, String subtitle, @Nullable ResourceLocation pageId,
        @Nullable Integer lineNumber, @Nullable GuidePageIcon icon, @Nullable String externalUrl, boolean header,
        boolean loadMoreRow) {

        public static RenderRow loadMore() {
            return new RenderRow("", "", null, null, null, null, false, true);
        }
    }

    @Desugar
    private record VisibilityCache(MediaWikiSpecialPageResult source, String normalizedQuery, int visibleCount,
        MediaWikiSpecialPageResult result) {

        private boolean matches(MediaWikiSpecialPageResult candidateSource, String candidateQuery,
            int candidateVisibleCount) {
            return source == candidateSource && normalizedQuery.equals(candidateQuery)
                && visibleCount == candidateVisibleCount;
        }
    }

    private static class RowLayout {

        private final LytRect bounds;
        @Nullable
        private final RenderRow row;
        private LytRect clickableBounds;
        @Nullable
        private GuideTooltip tooltip;

        private RowLayout(LytRect bounds, @Nullable RenderRow row) {
            this.bounds = bounds;
            this.row = row;
            this.clickableBounds = LytRect.empty();
            this.tooltip = null;
        }

        private LytRect bounds() {
            return bounds;
        }

        @Nullable
        private RenderRow row() {
            return row;
        }

        private LytRect clickableBounds() {
            return clickableBounds;
        }

        private void setClickableBounds(LytRect clickableBounds) {
            this.clickableBounds = clickableBounds != null ? clickableBounds : LytRect.empty();
        }

        private @Nullable GuideTooltip tooltip() {
            return tooltip;
        }

        private void setTooltip(@Nullable GuideTooltip tooltip) {
            this.tooltip = tooltip;
        }

        private RowLayout move(int deltaX, int deltaY) {
            RowLayout moved = new RowLayout(bounds.move(deltaX, deltaY), row);
            moved.setClickableBounds(clickableBounds.move(deltaX, deltaY));
            moved.setTooltip(tooltip);
            return moved;
        }
    }
}
