package com.hfstudio.guidenh.guide.document.block;

import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytDetailsBlock extends LytBlock implements InteractiveElement, LytBlockContainer {

    private static final ConstantColor SUMMARY_COLOR = new ConstantColor(0xFFE2E6ED);
    private static final String SUMMARY_OPEN_MARKER = "v";
    private static final String SUMMARY_CLOSED_MARKER = ">";
    private static final String DEFAULT_SUMMARY_TEXT = "Details";

    private final LytVBox root = new LytVBox();
    private final LytHBox summaryRow = new LytHBox();
    private final LytParagraph summaryMarker = new LytParagraph();
    private final LytParagraph summaryContent = new LytParagraph();
    private final LytVBox content = new LytVBox();

    private boolean open;
    @Nullable
    private String fallbackSummaryText;

    public LytDetailsBlock() {
        root.parent = this;
        root.setPadding(6);
        root.setGap(4);
        root.setFullWidth(true);
        root.setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        root.setBorder(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));

        summaryRow.parent = root;
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

        content.parent = root;
        content.setGap(4);
        content.setFullWidth(true);

        summaryRow.append(summaryMarker);
        summaryRow.append(summaryContent);
        root.append(summaryRow);
        root.append(content);
        syncSummaryMarker();
        syncContentVisibility();
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
        root.clearContent();
        root.append(summaryRow);
        if (open) {
            root.append(content);
        }
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
    public List<? extends LytNode> getChildren() {
        return root.getChildren();
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        return root.layout(context, x, y, availableWidth);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        root.moveLayoutPos(deltaX, deltaY);
    }

    @Override
    public void render(RenderContext context) {
        root.render(context);
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
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        return Optional.empty();
    }
}
