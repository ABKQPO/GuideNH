package com.hfstudio.guidenh.guide.document.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.flow.LytFlowContent;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;

public class LytQuoteBox extends LytBlock implements LytBlockContainer {

    private static final ColorValue DEFAULT_ACCENT = new ConstantColor(0xFF4FA3FF);

    private final LytVBox root = new LytVBox();
    private final LytParagraph titleParagraph = new LytParagraph();
    private final LytVBox content = new LytVBox();

    private boolean showTitle;

    public LytQuoteBox() {
        root.parent = this;
        root.setPadding(6);
        root.setGap(4);
        root.setFullWidth(true);
        root.setBackgroundColor(SymbolicColor.BLOCKQUOTE_BACKGROUND);
        root.setBorderLeft(new BorderStyle(DEFAULT_ACCENT, 3));

        titleParagraph.setMarginTop(0);
        titleParagraph.setMarginBottom(0);
        titleParagraph.modifyStyle(
            style -> style.bold(true)
                .color(DEFAULT_ACCENT));

        content.setGap(4);
        content.setFullWidth(true);
    }

    public void setQuoteStyle(ColorValue accentColor, @Nullable String title, @Nullable LytFlowContent icon) {
        ColorValue resolvedAccent = accentColor != null ? accentColor : DEFAULT_ACCENT;
        root.setBorderLeft(new BorderStyle(resolvedAccent, 3));
        titleParagraph.modifyStyle(
            style -> style.bold(true)
                .color(resolvedAccent));
        titleParagraph.clearContent();
        showTitle = (title != null && !title.isEmpty()) || icon != null;
        if (showTitle) {
            if (icon != null) {
                titleParagraph.append(icon);
                if (title != null && !title.isEmpty()) {
                    titleParagraph.appendText(" ");
                }
            }
            if (title != null && !title.isEmpty()) {
                titleParagraph.appendText(title);
            }
        }
        syncContentVisibility();
    }

    public ColorValue getAccentColor() {
        return root.getBorderLeft()
            .color();
    }

    public boolean hasTitleRow() {
        return showTitle;
    }

    private void syncContentVisibility() {
        root.clearContent();
        if (showTitle) {
            root.append(titleParagraph);
        }
        root.append(content);
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
}
