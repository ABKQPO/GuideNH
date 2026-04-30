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

    private final LytVBox root = new LytVBox();
    private final LytParagraph summary = new LytParagraph();
    private final LytVBox content = new LytVBox();

    private boolean open;

    public LytDetailsBlock() {
        root.parent = this;
        root.setPadding(6);
        root.setGap(4);
        root.setFullWidth(true);
        root.setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        root.setBorder(new BorderStyle(SymbolicColor.TABLE_BORDER, 1));

        summary.setMarginTop(0);
        summary.setMarginBottom(0);
        summary.modifyStyle(
            style -> style.bold(true)
                .color(SUMMARY_COLOR));

        content.setGap(4);
        content.setFullWidth(true);

        root.append(summary);
        root.append(content);
        syncContentVisibility();
    }

    public void setSummaryText(String text) {
        summary.clearContent();
        String marker = open ? "▼ " : "▶ ";
        summary.appendText(marker + (text != null && !text.isEmpty() ? text : "Details"));
    }

    public String getSummaryText() {
        String text = summary.getTextContent();
        if (text.startsWith("▼ ") || text.startsWith("▶ ")) {
            return text.substring(2);
        }
        return text;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        if (this.open != open) {
            this.open = open;
            setSummaryText(getSummaryText());
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

    private void syncContentVisibility() {
        root.clearContent();
        root.append(summary);
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
        root.setLayoutPos(
            root.getBounds()
                .point()
                .add(deltaX, deltaY));
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

        @Nullable
        LytRect summaryBounds = summary.getBounds();
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
