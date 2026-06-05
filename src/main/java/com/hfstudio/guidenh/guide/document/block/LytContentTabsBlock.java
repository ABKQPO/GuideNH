package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ContentTabsSpec;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytContentTabsBlock extends LytBlock implements InteractiveElement {

    private static final int HEADER_GAP = 4;
    private static final int HEADER_PAD_X = 8;
    private static final int HEADER_PAD_Y = 5;
    private static final int HEADER_RADIUS = 4;
    private static final int BODY_GAP = 6;
    private static final int SELECTED_FILL = 0xFF2E5C8A;
    private static final int SELECTED_BORDER = 0xFF8FC7FF;
    private static final int IDLE_FILL = 0xFF1F2430;
    private static final int IDLE_BORDER = 0xFF5A6372;
    private static final ResolvedTextStyle SELECTED_STYLE = new ResolvedTextStyle(
        1.0f,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        "",
        new ConstantColor(0xFFFFFFFF),
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false);
    private static final ResolvedTextStyle IDLE_STYLE = new ResolvedTextStyle(
        1.0f,
        false,
        false,
        false,
        false,
        false,
        false,
        false,
        "",
        new ConstantColor(0xFFD8DEEA),
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false);

    private final List<TabState> tabs = new ArrayList<>();
    private int selectedIndex;
    private LytRect headerBounds = LytRect.empty();

    public LytContentTabsBlock(int selectedIndex, List<ContentTabsSpec.TabEntry> entries) {
        this.selectedIndex = Math.max(0, selectedIndex);
        for (ContentTabsSpec.TabEntry entry : entries) {
            tabs.add(new TabState(entry.title(), entry.body()));
            entry.body().parent = this;
        }
        setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        setFullWidth(true);
    }

    @Override
    public List<? extends LytNode> getChildren() {
        return tabs.isEmpty() ? List.of() : List.of(tabs.get(selectedIndex).body);
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (tabs.isEmpty()) {
            headerBounds = LytRect.empty();
            return new LytRect(x, y, 0, 0);
        }

        selectedIndex = Math.max(0, Math.min(selectedIndex, tabs.size() - 1));

        int cursorX = x;
        int cursorY = y;
        int rowHeight = 0;
        int maxRight = x;
        int headerBottom = y;

        for (TabState tab : tabs) {
            int tabWidth = tab.measureWidth(context);
            int tabHeight = tab.measureHeight(context);
            if (cursorX > x && cursorX + tabWidth > x + availableWidth) {
                cursorX = x;
                cursorY += rowHeight + HEADER_GAP;
                rowHeight = 0;
            }
            tab.bounds = new LytRect(cursorX, cursorY, tabWidth, tabHeight);
            cursorX += tabWidth + HEADER_GAP;
            rowHeight = Math.max(rowHeight, tabHeight);
            maxRight = Math.max(maxRight, tab.bounds.right());
            headerBottom = Math.max(headerBottom, tab.bounds.bottom());
        }

        headerBounds = new LytRect(x, y, Math.max(0, maxRight - x), Math.max(0, headerBottom - y));
        LytBlock activeBody = tabs.get(selectedIndex).body;
        LytRect bodyBounds = activeBody.layout(context, x, headerBounds.bottom() + BODY_GAP, availableWidth);
        return new LytRect(x, y, Math.max(headerBounds.width(), bodyBounds.width()), bodyBounds.bottom() - y);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        headerBounds = headerBounds.move(deltaX, deltaY);
        for (TabState tab : tabs) {
            tab.bounds = tab.bounds.move(deltaX, deltaY);
            tab.body.moveLayoutPos(deltaX, deltaY);
        }
    }

    @Override
    public void render(RenderContext context) {
        if (tabs.isEmpty()) {
            return;
        }
        for (int index = 0; index < tabs.size(); index++) {
            TabState tab = tabs.get(index);
            boolean selected = index == selectedIndex;
            context.fillRoundedRect(tab.bounds, selected ? SELECTED_FILL : IDLE_FILL, HEADER_RADIUS);
            context.drawRoundedBorder(tab.bounds, selected ? SELECTED_BORDER : IDLE_BORDER, 1, HEADER_RADIUS);
            context
                .drawText(tab.title, tab.bounds.x() + HEADER_PAD_X, tab.bounds.y() + HEADER_PAD_Y, tab.style(selected));
        }
        tabs.get(selectedIndex).body.render(context);
    }

    @Override
    public boolean mouseClicked(GuideUiHost screen, int x, int y, int button, boolean doubleClick) {
        if (tabs.isEmpty()) {
            return false;
        }
        if (button == 0) {
            for (int index = 0; index < tabs.size(); index++) {
                if (tabs.get(index).bounds.contains(x, y)) {
                    if (selectedIndex != index) {
                        selectedIndex = index;
                        if (getDocument() != null) {
                            getDocument().invalidateLayout();
                        }
                    }
                    return true;
                }
            }
        }
        LytBlock activeBody = tabs.get(selectedIndex).body;
        return activeBody instanceof InteractiveElement interactive
            && interactive.mouseClicked(screen, x, y, button, doubleClick);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (tabs.isEmpty()) {
            return Optional.empty();
        }
        LytBlock activeBody = tabs.get(selectedIndex).body;
        return activeBody instanceof InteractiveElement interactive ? interactive.getTooltip(x, y) : Optional.empty();
    }

    private static class TabState {

        private final String title;
        private final LytBlock body;
        private LytRect bounds = LytRect.empty();

        private TabState(String title, LytBlock body) {
            this.title = title;
            this.body = body;
        }

        private int measureWidth(LayoutContext context) {
            return context.getWidth(title, style(false)) + HEADER_PAD_X * 2;
        }

        private int measureHeight(LayoutContext context) {
            return context.getLineHeight(style(false)) + HEADER_PAD_Y * 2;
        }

        private ResolvedTextStyle style(boolean selected) {
            return selected ? SELECTED_STYLE : IDLE_STYLE;
        }
    }
}
