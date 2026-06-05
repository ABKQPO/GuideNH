package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.color.SymbolicColor;
import com.hfstudio.guidenh.guide.compiler.PageCompiler;
import com.hfstudio.guidenh.guide.compiler.tags.ContentTabsSpec;
import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.document.interaction.GuideTooltip;
import com.hfstudio.guidenh.guide.document.interaction.InteractiveElement;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.BorderStyle;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;
import com.hfstudio.guidenh.guide.style.TextAlignment;
import com.hfstudio.guidenh.guide.style.WhiteSpaceMode;
import com.hfstudio.guidenh.guide.ui.GuideUiHost;

public class LytContentTabsBlock extends LytBlock implements InteractiveElement {

    private static final int ACCENT_WIDTH = 3;
    private static final int CONTAINER_PAD_X = 10;
    private static final int CONTAINER_PAD_Y = 6;
    private static final int HEADER_GAP_X = 10;
    private static final int HEADER_GAP_Y = 5;
    private static final int HEADER_PAD_X = 2;
    private static final int HEADER_PAD_TOP = 1;
    private static final int HEADER_PAD_BOTTOM = 5;
    private static final int HEADER_RULE_THICKNESS = 1;
    private static final int ACTIVE_RULE_THICKNESS = 2;
    private static final int BODY_GAP = 6;
    private static final ConstantColor DEFAULT_ACCENT = new ConstantColor(0xFF7C8795);
    private static final int HEADER_RULE_COLOR = 0x66586275;
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
        new ConstantColor(0xFFF4F7FB),
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
        new ConstantColor(0xFFD5DCE7),
        WhiteSpaceMode.NORMAL,
        TextAlignment.LEFT,
        false,
        null,
        false);

    private final List<TabState> tabs = new ArrayList<>();
    private final List<LytBlock> tabBodies = new ArrayList<>();
    private final ColorValue accentColor;
    private int selectedIndex;
    private LytRect headerBounds = LytRect.empty();
    private LytRect contentBounds = LytRect.empty();

    public LytContentTabsBlock(int selectedIndex, @Nullable ColorValue accentColor,
        List<ContentTabsSpec.TabEntry> entries) {
        this.accentColor = accentColor != null ? accentColor : DEFAULT_ACCENT;
        this.selectedIndex = Math.max(0, selectedIndex);
        for (ContentTabsSpec.TabEntry entry : entries) {
            tabs.add(new TabState(entry.title(), entry.body()));
            tabBodies.add(entry.body());
            entry.body().parent = this;
        }
        setMarginTop(PageCompiler.DEFAULT_ELEMENT_SPACING);
        setMarginBottom(PageCompiler.DEFAULT_ELEMENT_SPACING);
        setFullWidth(true);
        setBorderLeft(new BorderStyle(this.accentColor, ACCENT_WIDTH));
    }

    @Override
    public List<? extends LytNode> getChildren() {
        // Expose every tab body to tree visitors so search, anchors, resource export,
        // scene collection, and mount-time traversal still see hidden tabs.
        return tabBodies;
    }

    @Override
    protected LytRect computeLayout(LayoutContext context, int x, int y, int availableWidth) {
        if (tabs.isEmpty()) {
            headerBounds = LytRect.empty();
            contentBounds = LytRect.empty();
            return new LytRect(x, y, 0, 0);
        }

        selectedIndex = Math.clamp(selectedIndex, 0, tabs.size() - 1);

        int contentX = x + ACCENT_WIDTH + CONTAINER_PAD_X;
        int contentY = y + CONTAINER_PAD_Y;
        int contentWidth = Math.max(0, availableWidth - ACCENT_WIDTH - CONTAINER_PAD_X * 2);
        int cursorX = contentX;
        int cursorY = contentY;
        int rowHeight = 0;
        int headerBottom = contentY;

        for (TabState tab : tabs) {
            int tabWidth = tab.measureWidth(context);
            int tabHeight = tab.measureHeight(context);
            if (cursorX > contentX && cursorX + tabWidth > contentX + contentWidth) {
                cursorX = contentX;
                cursorY += rowHeight + HEADER_GAP_Y;
                rowHeight = 0;
            }
            tab.bounds = new LytRect(cursorX, cursorY, tabWidth, tabHeight);
            cursorX += tabWidth + HEADER_GAP_X;
            rowHeight = Math.max(rowHeight, tabHeight);
            headerBottom = Math.max(headerBottom, tab.bounds.bottom());
        }

        headerBounds = new LytRect(
            contentX,
            contentY,
            contentWidth,
            Math.max(0, headerBottom - contentY));
        int safeSelectedIndex = getSafeSelectedIndex();
        LytBlock activeBody = tabs.get(safeSelectedIndex).body;
        LytRect bodyBounds = activeBody.layout(context, contentX, headerBounds.bottom() + BODY_GAP, contentWidth);
        int contentRight = Math.max(headerBounds.right(), bodyBounds.right());
        int contentBottom = Math.max(headerBounds.bottom(), bodyBounds.bottom());
        contentBounds = new LytRect(
            contentX,
            contentY,
            Math.max(0, contentRight - contentX),
            Math.max(0, contentBottom - contentY));
        return new LytRect(
            x,
            y,
            Math.max(availableWidth, ACCENT_WIDTH + CONTAINER_PAD_X * 2 + contentBounds.width()),
            contentBounds.height() + CONTAINER_PAD_Y * 2);
    }

    @Override
    protected void onLayoutMoved(int deltaX, int deltaY) {
        headerBounds = headerBounds.move(deltaX, deltaY);
        contentBounds = contentBounds.move(deltaX, deltaY);
        for (TabState tab : tabs) {
            tab.bounds = tab.bounds.move(deltaX, deltaY);
        }
        if (!tabs.isEmpty()) {
            tabs.get(getSafeSelectedIndex()).body.moveLayoutPos(deltaX, deltaY);
        }
    }

    @Override
    public void render(RenderContext context) {
        if (tabs.isEmpty()) {
            return;
        }
        int safeSelectedIndex = getSafeSelectedIndex();
        int accentArgb = context.resolveColor(accentColor);
        context.fillRect(bounds, context.resolveColor(SymbolicColor.BLOCKQUOTE_BACKGROUND));
        context.fillRect(bounds.x(), bounds.y(), ACCENT_WIDTH, bounds.height(), accentArgb);
        float panelRuleY = headerBounds.bottom() + HEADER_RULE_THICKNESS * 0.5f;
        context.drawLine(
            headerBounds.x(),
            panelRuleY,
            headerBounds.right(),
            panelRuleY,
            HEADER_RULE_THICKNESS,
            HEADER_RULE_COLOR);
        for (int index = 0; index < tabs.size(); index++) {
            TabState tab = tabs.get(index);
            boolean selected = index == safeSelectedIndex;
            context
                .drawText(tab.title, tab.bounds.x() + HEADER_PAD_X, tab.bounds.y() + HEADER_PAD_TOP, tab.style(selected));
            if (selected) {
                float activeRuleY = tab.bounds.bottom() - ACTIVE_RULE_THICKNESS * 0.5f;
                context.drawLine(
                    tab.bounds.x(),
                    activeRuleY,
                    tab.bounds.right(),
                    activeRuleY,
                    ACTIVE_RULE_THICKNESS,
                    accentArgb);
            }
        }
        tabs.get(safeSelectedIndex).body.render(context);
    }

    @Override
    public @Nullable LytNode pickNode(int x, int y) {
        if (!bounds.contains(x, y)) {
            return null;
        }
        for (TabState tab : tabs) {
            if (tab.bounds.contains(x, y)) {
                return this;
            }
        }
        if (!tabs.isEmpty()) {
            LytNode activeNode = tabs.get(getSafeSelectedIndex()).body.pickNode(x, y);
            if (activeNode != null) {
                return activeNode;
            }
        }
        return this;
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
        LytBlock activeBody = tabs.get(getSafeSelectedIndex()).body;
        return activeBody instanceof InteractiveElement interactive
            && interactive.mouseClicked(screen, x, y, button, doubleClick);
    }

    @Override
    public Optional<GuideTooltip> getTooltip(float x, float y) {
        if (tabs.isEmpty()) {
            return Optional.empty();
        }
        LytBlock activeBody = tabs.get(getSafeSelectedIndex()).body;
        return activeBody instanceof InteractiveElement interactive ? interactive.getTooltip(x, y) : Optional.empty();
    }

    private int getSafeSelectedIndex() {
        if (tabs.isEmpty()) {
            return 0;
        }
        return Math.clamp(selectedIndex, 0, tabs.size() - 1);
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
            return context.getStringWidth(title, style(false)) + HEADER_PAD_X * 2;
        }

        private int measureHeight(LayoutContext context) {
            return context.getLineHeight(style(false)) + HEADER_PAD_TOP + HEADER_PAD_BOTTOM;
        }

        private ResolvedTextStyle style(boolean selected) {
            return selected ? SELECTED_STYLE : IDLE_STYLE;
        }
    }
}
