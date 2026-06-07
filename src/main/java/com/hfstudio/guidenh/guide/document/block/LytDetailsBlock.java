package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.DocumentDragTarget;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorVerticalScrollbar;
import com.hfstudio.guidenh.guide.internal.util.SmoothFloatState;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytDetailsBlock extends LytBlock implements InteractiveElement, LytBlockContainer, DocumentDragTarget {

    private static final ConstantColor SUMMARY_COLOR = new ConstantColor(0xFFE2E6ED);
    private static final String SUMMARY_OPEN_MARKER = "v";
    private static final String SUMMARY_CLOSED_MARKER = ">";
    private static final String DEFAULT_SUMMARY_TEXT = "Details";
    private static final int PADDING = 6;
    private static final int GAP = 4;
    private static final int BORDER_WIDTH = 1;
    private static final int SCROLLBAR_WIDTH = 5;
    private static final int SCROLLBAR_GAP = 4;
    private static final int MIN_SCROLLBAR_THUMB = 14;
    private static final int MIN_WHEEL_STEP = 16;
    private static final BorderStyle DETAILS_BORDER = new BorderStyle(SymbolicColor.TABLE_BORDER, BORDER_WIDTH);

    private final LytHBox summaryRow = new LytHBox();
    private final LytParagraph summaryMarker = new LytParagraph();
    private final LytParagraph summaryContent = new LytParagraph();
    private final LytVBox content = new LytVBox();
    private final BorderRenderer borderRenderer = new BorderRenderer();
    private final SmoothFloatState visualContentScrollOffsetY = new SmoothFloatState();

    private boolean open;
    @Nullable
    private String fallbackSummaryText;
    private int preferredWidth;
    private int preferredContentHeight;
    private int contentHeight;
    private int contentViewportX;
    private int contentViewportY;
    private int contentViewportWidth;
    private int contentViewportHeight;
    private int contentScrollOffsetY;
    private boolean draggingContent;
    private int dragLastDocumentY;
    private boolean draggingScrollbar;
    private int scrollbarGrabOffsetY;

    public LytDetailsBlock() {
        summaryRow.parent = this;
        summaryRow.setGap(4);
        summaryRow.setWrap(false);
        summaryRow.setFullWidth(true);
        summaryRow.setAlignItems(AlignItems.CENTER);

        summaryMarker.setMarginTop(0);
        summaryMarker.setMarginBottom(0);
        summaryMarker.modifyStyle(
            style -> style.bold(true)
                .color(SUMMARY_COLOR));

        summaryContent.setMarginTop(0);
        summaryContent.setMarginBottom(0);
        summaryContent.modifyStyle(
            style -> style.bold(true)
                .color(SUMMARY_COLOR));

        content.parent = this;
        content.setGap(4);
        content.setFullWidth(true);

        summaryRow.append(summaryMarker);
        summaryRow.append(summaryContent);
        syncSummaryMarker();
        syncContentVisibility();
    }

    public int getPreferredWidth() {
        return preferredWidth;
    }

    public void setPreferredWidth(int preferredWidth) {
        this.preferredWidth = Math.max(0, preferredWidth);
        setFullWidth(this.preferredWidth <= 0);
    }

    public int getPreferredContentHeight() {
        return preferredContentHeight;
    }

    public void setPreferredContentHeight(int preferredContentHeight) {
        this.preferredContentHeight = Math.max(0, preferredContentHeight);
    }

    public LytParagraph getSummaryBox() {
        return summaryContent;
    }

    public void setFallbackSummaryText(@Nullable String fallbackSummaryText) {
        this.fallbackSummaryText = fallbackSummaryText;
        syncSummaryFallback();
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        if (this.open != open) {
            this.open = open;
            syncSummaryMarker();
            syncContentVisibility();
            var document = getDocument();
            if (document != null) {
                document.invalidateLayout();
            }
        }
    }

    public LytVBox getContentBox() {
        return content;
    }

    private void syncSummaryMarker() {
        summaryMarker.clearContent();
        summaryMarker.appendText(open ? SUMMARY_OPEN_MARKER : SUMMARY_CLOSED_MARKER);
    }

    private void syncSummaryFallback() {
        if (!summaryContent.isEmpty()) {
            return;
        }
        summaryContent.clearContent();
        summaryContent.appendText(
            fallbackSummaryText != null && !fallbackSummaryText.trim()
                .isEmpty() ? fallbackSummaryText : DEFAULT_SUMMARY_TEXT);
    }

    private void syncContentVisibility() {
        syncSummaryFallback();
    }

    @Override
    public void append(LytBlock block) {
        content.append(block);
    }

    @Override
    public void removeChild(LytNode node) {
        content.removeChild(node);
    }

    @Override
    public void replaceChild(LytNode oldChild, LytNode newChild) {
        content.replaceChild(oldChild, newChild);
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return open ? List.of(summaryRow, content) : List.of(summaryRow);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        int safeWidth = preferredWidth > 0 ? Math.max(1, Math.min(availableWidth, preferredWidth))
            : Math.max(1, availableWidth);
        int innerX = x + PADDING + BORDER_WIDTH;
        int innerY = y + PADDING + BORDER_WIDTH;
        int innerWidth = Math.max(1, safeWidth - (PADDING + BORDER_WIDTH) * 2);

        LytRect summaryBounds = summaryRow.layout(context, innerX, innerY, innerWidth);
        int totalHeight = PADDING + BORDER_WIDTH + summaryBounds.height() + PADDING + BORDER_WIDTH;

        contentHeight = 0;
        contentViewportX = innerX;
        contentViewportY = summaryBounds.bottom() + GAP;
        contentViewportWidth = innerWidth;
        contentViewportHeight = 0;

        if (open) {
            LytRect measuredContent = content.layout(context, contentViewportX, contentViewportY, contentViewportWidth);
            contentHeight = measuredContent.height();
            contentViewportHeight = preferredContentHeight > 0 ? preferredContentHeight : contentHeight;
            if (preferredContentHeight > 0 && contentHeight > contentViewportHeight) {
                contentViewportWidth = Math.max(1, innerWidth - SCROLLBAR_WIDTH - SCROLLBAR_GAP);
                measuredContent = content.layout(context, contentViewportX, contentViewportY, contentViewportWidth);
                contentHeight = measuredContent.height();
            }
            contentViewportHeight = preferredContentHeight > 0 ? preferredContentHeight : contentHeight;
            setContentScrollOffset(contentScrollOffsetY);
            snapVisualScrollToTarget();
            totalHeight = PADDING + BORDER_WIDTH
                + summaryBounds.height()
                + GAP
                + contentViewportHeight
                + PADDING
                + BORDER_WIDTH;
        } else {
            setContentScrollOffset(0);
            snapVisualScrollToTarget();
        }

        return new LytRect(x, y, safeWidth, totalHeight);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        summaryRow.moveLayoutPos(deltaX, deltaY);
        content.moveLayoutPos(deltaX, deltaY);
        contentViewportX += deltaX;
        contentViewportY += deltaY;
    }

    @Override
    public void render(RenderContext context) {
        updateVisualScroll();
        context.fillRect(bounds, SymbolicColor.BLOCKQUOTE_BACKGROUND);
        summaryRow.render(context);
        if (open) {
            LytRect viewport = getContentViewportBounds();
            context.pushLocalScissor(viewport);
            try {
                renderContentWithVisualOffset(context);
            } finally {
                context.popScissor();
            }
            renderScrollbar(context);
        }
        borderRenderer.render(context, bounds, DETAILS_BORDER, DETAILS_BORDER, DETAILS_BORDER, DETAILS_BORDER);
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (button != 0) {
            return false;
        }

        LytRect summaryBounds = summaryRow.getBounds();
        if (summaryBounds != null && summaryBounds.contains(x, y)) {
            setOpen(!open);
            return true;
        }
        return false;
    }

    @Override
    public boolean beginDrag(int documentX, int documentY, int button) {
        if (!open || button != 0 || getMaxContentScroll() <= 0) {
            return false;
        }
        if (getScrollbarTrackBounds().contains(documentX, documentY)) {
            LytRect thumbBounds = getScrollbarThumbBounds();
            if (!thumbBounds.isEmpty() && thumbBounds.contains(documentX, documentY)) {
                scrollbarGrabOffsetY = documentY - thumbBounds.y();
            } else {
                scrollbarGrabOffsetY = thumbBounds.isEmpty() ? 0 : thumbBounds.height() / 2;
                updateScrollFromMouseY(documentY);
            }
            draggingScrollbar = true;
            draggingContent = false;
            return true;
        }
        if (!getContentViewportBounds().contains(documentX, documentY)) {
            return false;
        }
        draggingContent = true;
        draggingScrollbar = false;
        dragLastDocumentY = documentY;
        return true;
    }

    @Override
    public void dragTo(int documentX, int documentY) {
        if (draggingScrollbar) {
            updateScrollFromMouseY(documentY);
            return;
        }
        if (!draggingContent) {
            return;
        }
        int deltaY = documentY - dragLastDocumentY;
        dragLastDocumentY = documentY;
        setContentScrollOffset(contentScrollOffsetY - deltaY);
    }

    @Override
    public void endDrag() {
        draggingContent = false;
        draggingScrollbar = false;
    }

    @Override
    public boolean scroll(int documentX, int documentY, int wheelDelta) {
        if (!open || wheelDelta == 0
            || getMaxContentScroll() <= 0
            || !getContentViewportBounds().contains(documentX, documentY)) {
            return false;
        }
        setContentScrollOffset(contentScrollOffsetY - Integer.signum(wheelDelta) * MIN_WHEEL_STEP);
        return true;
    }

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        if (!bounds.contains(x, y)) {
            return null;
        }
        if (summaryRow.getBounds() != null && summaryRow.getBounds()
            .contains(x, y)) {
            LytNode node = summaryRow.pickNode(x, y);
            return node != null ? node : this;
        }
        if (open && getScrollbarTrackBounds().contains(x, y)) {
            return this;
        }
        if (open && getContentViewportBounds().contains(x, y)) {
            LytNode node = content.pickNode(x, y);
            return node != null ? node : this;
        }
        return this;
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        return Optional.empty();
    }

    private void renderScrollbar(RenderContext context) {
        LytRect trackBounds = getScrollbarTrackBounds();
        if (trackBounds.isEmpty()) {
            return;
        }
        context.fillRect(trackBounds, 0x30242B33);
        LytRect thumbBounds = getScrollbarThumbBounds();
        if (!thumbBounds.isEmpty()) {
            context.fillRect(thumbBounds, draggingScrollbar ? 0xFFCDD6E1 : 0xA0AAB5C2);
        }
    }

    private LytRect getContentViewportBounds() {
        return new LytRect(contentViewportX, contentViewportY, contentViewportWidth, contentViewportHeight);
    }

    private LytRect getScrollbarTrackBounds() {
        if (getMaxContentScroll() <= 0) {
            return LytRect.empty();
        }
        return new LytRect(
            contentViewportX + contentViewportWidth + SCROLLBAR_GAP,
            contentViewportY,
            SCROLLBAR_WIDTH,
            contentViewportHeight);
    }

    private LytRect getScrollbarThumbBounds() {
        LytRect track = getScrollbarTrackBounds();
        if (track.isEmpty()) {
            return LytRect.empty();
        }
        int thumbHeight = Math
            .max(MIN_SCROLLBAR_THUMB, track.height() * track.height() / Math.max(track.height(), contentHeight));
        thumbHeight = Math.min(thumbHeight, track.height());
        int maxScroll = getMaxContentScroll();
        int thumbTrack = Math.max(1, track.height() - thumbHeight);
        int thumbY = track.y();
        if (maxScroll > 0) {
            thumbY += (int) ((long) thumbTrack * visualContentScrollOffsetY.rounded() / maxScroll);
        }
        return new LytRect(track.x(), thumbY, track.width(), thumbHeight);
    }

    private int getMaxContentScroll() {
        return Math.max(0, contentHeight - contentViewportHeight);
    }

    private void setContentScrollOffset(int contentScrollOffsetY) {
        this.contentScrollOffsetY = SceneEditorVerticalScrollbar.clamp(contentScrollOffsetY, 0, getMaxContentScroll());
        updateContentPosition();
    }

    private void updateContentPosition() {
        if (!content.getBounds()
            .isEmpty()) {
            content.moveLayoutPos(
                contentViewportX - content.getBounds()
                    .x(),
                contentViewportY - contentScrollOffsetY
                    - content.getBounds()
                        .y());
        }
    }

    private void renderContentWithVisualOffset(RenderContext context) {
        int renderDeltaY = contentScrollOffsetY - visualContentScrollOffsetY.rounded();
        if (renderDeltaY == 0) {
            content.render(context);
            return;
        }
        content.moveLayoutPos(0, renderDeltaY);
        try {
            content.render(context);
        } finally {
            content.moveLayoutPos(0, -renderDeltaY);
        }
    }

    private void updateScrollFromMouseY(int mouseY) {
        LytRect track = getScrollbarTrackBounds();
        LytRect thumb = getScrollbarThumbBounds();
        if (track.isEmpty() || thumb.isEmpty()) {
            setContentScrollOffset(0);
            return;
        }
        int thumbTrack = Math.max(1, track.height() - thumb.height());
        int thumbTop = SceneEditorVerticalScrollbar
            .clamp(mouseY - scrollbarGrabOffsetY, track.y(), track.y() + thumbTrack);
        int maxScroll = getMaxContentScroll();
        setContentScrollOffset((int) ((long) (thumbTop - track.y()) * maxScroll / thumbTrack));
    }

    private void snapVisualScrollToTarget() {
        visualContentScrollOffsetY.snapTo(contentScrollOffsetY);
    }

    private void updateVisualScroll() {
        visualContentScrollOffsetY
            .updateTowards(contentScrollOffsetY, 28f, 0.25f, 0.01f, Math.max(128f, contentViewportHeight * 2f));
    }
}
