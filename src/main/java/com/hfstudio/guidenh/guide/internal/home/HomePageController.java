package com.hfstudio.guidenh.guide.internal.home;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.internal.screen.GuideNavBar;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;

public class HomePageController {

    private static final int SECTION_PADDING = 8;
    private static final int TITLE_GAP = 8;
    private static final int ROW_HEIGHT = 32;
    private static final int ROW_GAP = 4;
    private static final int ICON_GAP = 6;
    private static final int SCROLLBAR_WIDTH = 6;
    private static final int SCROLLBAR_GAP = 4;
    private static final int DRAG_THRESHOLD = 3;
    private static final float ENTRY_TITLE_SCALE = 1.12f;
    private static final float ENTRY_SUMMARY_SCALE = 0.85f;
    private static final int PANEL_COLOR = 0xA6181A20;
    private static final int ROW_COLOR = 0x661E232B;
    private static final int ROW_HOVER_COLOR = 0x88303946;
    private static final int TITLE_COLOR = 0xFFE5E9F0;
    private static final int EMPTY_COLOR = 0xFF9AA3B2;
    private static final int SUMMARY_COLOR = 0xFF9AA3B2;
    private static final int ENTRY_TEXT_TOP = 4;
    private static final int ENTRY_TEXT_BOTTOM = 25;

    private int recommendedScrollOffset;
    private int bookmarksScrollOffset;
    private int historyScrollOffset;

    @Nullable
    private PendingClick pendingClick;
    @Nullable
    private DragState dragState;

    public void render(Minecraft mc, HomePageDataBuilder.HomePageSections sections, HomePageLayout.LayoutRects layout,
        ResourceLocation logoTexture, int mouseX, int mouseY) {
        drawLogo(mc, layout.logo(), logoTexture);
        drawSection(mc, layout.recommended(), sections.recommended(), mouseX, mouseY, layout.recommendedTitleSafeTop());
        drawSection(mc, layout.bookmarks(), sections.bookmarks(), mouseX, mouseY, 0);
        drawSection(mc, layout.history(), sections.history(), mouseX, mouseY, 0);
    }

    public boolean mouseWheel(HomePageDataBuilder.HomePageSections sections, HomePageLayout.LayoutRects layout,
        int mouseX, int mouseY, int wheelDelta) {
        SectionTarget target = findSectionTarget(sections, layout, mouseX, mouseY);
        if (target == null) {
            return false;
        }
        int step = Math.max(12, ROW_HEIGHT - 4);
        int next = getScrollOffset(target.section()) - Integer.signum(wheelDelta) * step;
        setScrollOffset(target.section(), clampScroll(target.section(), target.rect(), next, target.titleTopInset()));
        return true;
    }

    public boolean mousePressed(HomePageDataBuilder.HomePageSections sections, HomePageLayout.LayoutRects layout,
        int mouseX, int mouseY) {
        SectionTarget target = findSectionTarget(sections, layout, mouseX, mouseY);
        if (target == null) {
            pendingClick = null;
            dragState = null;
            return false;
        }

        HomePageEntry entry = findEntryAt(target, mouseX, mouseY);
        if (entry != null) {
            pendingClick = new PendingClick(entry, mouseX, mouseY, false);
        } else {
            pendingClick = null;
        }
        SectionMetrics metrics = metricsFor(target.rect(), target.titleTopInset());
        if (isInsideScrollbar(target.rect(), metrics, mouseX, mouseY, target.section())) {
            int grabOffset = scrollbarThumbOffset(target.rect(), metrics, mouseY, target.section());
            dragState = new DragState(
                target.section(),
                target.rect(),
                target.titleTopInset(),
                mouseY,
                true,
                grabOffset);
        } else {
            dragState = new DragState(target.section(), target.rect(), target.titleTopInset(), mouseY, false, 0);
        }
        return true;
    }

    public boolean mouseDragged(int mouseX, int mouseY) {
        boolean handled = false;
        if (pendingClick != null) {
            int dx = Math.abs(mouseX - pendingClick.startMouseX());
            int dy = Math.abs(mouseY - pendingClick.startMouseY());
            if (dx > DRAG_THRESHOLD || dy > DRAG_THRESHOLD) {
                pendingClick = pendingClick.withDragged(true);
            }
        }
        if (dragState != null) {
            if (dragState.scrollbar()) {
                setScrollOffset(
                    dragState.section(),
                    resolveScrollbarScrollOffset(
                        dragState.section(),
                        dragState.rect(),
                        dragState.titleTopInset(),
                        mouseY,
                        dragState.grabOffset()));
            } else {
                int deltaY = mouseY - dragState.lastMouseY();
                int next = getScrollOffset(dragState.section()) - deltaY;
                setScrollOffset(
                    dragState.section(),
                    clampScroll(dragState.section(), dragState.rect(), next, dragState.titleTopInset()));
            }
            dragState = dragState.withLastMouseY(mouseY);
            handled = true;
        }
        return handled;
    }

    @Nullable
    public HomePageEntry mouseReleased(HomePageDataBuilder.HomePageSections sections, HomePageLayout.LayoutRects layout,
        int mouseX, int mouseY) {
        try {
            if (pendingClick != null && !pendingClick.dragged()) {
                SectionTarget target = findSectionTarget(sections, layout, mouseX, mouseY);
                HomePageEntry releasedEntry = target != null ? findEntryAt(target, mouseX, mouseY) : null;
                if (releasedEntry == pendingClick.entry()) {
                    return pendingClick.entry();
                }
            }
            return null;
        } finally {
            pendingClick = null;
            dragState = null;
        }
    }

    private void drawLogo(Minecraft mc, HomePageLayout.Rect rect, ResourceLocation logoTexture) {
        mc.getTextureManager()
            .bindTexture(logoTexture);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(rect.x(), rect.y() + rect.height(), 0, 0f, 1f);
        tessellator.addVertexWithUV(rect.x() + rect.width(), rect.y() + rect.height(), 0, 1f, 1f);
        tessellator.addVertexWithUV(rect.x() + rect.width(), rect.y(), 0, 1f, 0f);
        tessellator.addVertexWithUV(rect.x(), rect.y(), 0, 0f, 0f);
        tessellator.draw();
    }

    private void drawSection(Minecraft mc, HomePageLayout.Rect rect, HomePageSection section, int mouseX, int mouseY,
        int topInset) {
        Gui.drawRect(rect.x(), rect.y(), rect.x() + rect.width(), rect.y() + rect.height(), PANEL_COLOR);

        FontRenderer font = mc.fontRenderer;
        SectionMetrics metrics = metricsFor(rect, topInset);
        int titleY = metrics.titleY();
        font.drawString(section.title(), rect.x() + SECTION_PADDING, titleY, TITLE_COLOR, false);

        int contentX = metrics.contentX();
        int contentY = metrics.contentY();
        int scrollbarX = metrics.scrollbarX();
        int rowWidth = Math.max(30, scrollbarX - SCROLLBAR_GAP - contentX);
        int visibleHeight = metrics.visibleHeight();

        if (section.entries()
            .isEmpty()) {
            drawCenteredEmptyMessage(font, section.emptyText(), rect);
            return;
        }

        int contentHeight = computeContentHeight(section);
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        int scrollOffset = clampScroll(section, rect, getScrollOffset(section), topInset);
        setScrollOffset(section, scrollOffset);

        pushScissor(contentX, contentY, rowWidth, visibleHeight);
        for (int i = 0; i < section.entries()
            .size(); i++) {
            HomePageEntry entry = section.entries()
                .get(i);
            int rowY = contentY + i * (ROW_HEIGHT + ROW_GAP) - scrollOffset;
            if (rowY + ROW_HEIGHT < contentY || rowY > contentY + visibleHeight) {
                continue;
            }
            drawRow(mc, entry, contentX, rowY, rowWidth, mouseX, mouseY);
        }
        popScissor();

        if (maxScroll > 0) {
            drawScrollbar(rect, section, scrollOffset, metrics, contentHeight);
        }
    }

    private void drawRow(Minecraft mc, HomePageEntry entry, int x, int y, int width, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + ROW_HEIGHT;
        Gui.drawRect(x, y, x + width, y + ROW_HEIGHT, hovered ? ROW_HOVER_COLOR : ROW_COLOR);

        FontRenderer font = mc.fontRenderer;
        int textX = x + 4;
        int titleY = y + ENTRY_TEXT_TOP;
        int summaryY = y + 18;
        if (entry.icon() != null) {
            int iconSize = ENTRY_TEXT_BOTTOM - ENTRY_TEXT_TOP;
            int iconY = y + (ROW_HEIGHT - iconSize) / 2;
            GuideNavBar.drawMiniIcon(mc, entry.icon(), x + 4, iconY, iconSize);
            textX += iconSize + ICON_GAP;
        }

        int textWidth = width - (textX - x) - 4;
        drawScaledTrimmedString(font, entry.title(), textX, titleY, textWidth, TITLE_COLOR, ENTRY_TITLE_SCALE);
        drawScaledTrimmedString(font, entry.summary(), textX, summaryY, textWidth, SUMMARY_COLOR, ENTRY_SUMMARY_SCALE);
    }

    private void drawScrollbar(HomePageLayout.Rect rect, HomePageSection section, int scrollOffset,
        SectionMetrics metrics, int contentHeight) {
        int visibleHeight = metrics.visibleHeight();
        int maxScroll = Math.max(0, contentHeight - visibleHeight);
        if (maxScroll <= 0) {
            return;
        }
        int x = metrics.scrollbarX();
        int contentY = metrics.contentY();
        int thumbHeight = Math.max(18, visibleHeight * visibleHeight / Math.max(visibleHeight, contentHeight));
        int travel = Math.max(1, visibleHeight - thumbHeight);
        int thumbY = contentY + (int) ((long) scrollOffset * travel / maxScroll);
        Gui.drawRect(x, contentY, x + SCROLLBAR_WIDTH, contentY + visibleHeight, 0x22262D38);
        Gui.drawRect(x, thumbY, x + SCROLLBAR_WIDTH, thumbY + thumbHeight, 0x889AA3B2);
    }

    private int computeContentHeight(HomePageSection section) {
        if (section.entries()
            .isEmpty()) {
            return 0;
        }
        return section.entries()
            .size() * ROW_HEIGHT
            + (section.entries()
                .size() - 1) * ROW_GAP;
    }

    private int clampScroll(HomePageSection section, HomePageLayout.Rect rect, int next, int topInset) {
        SectionMetrics metrics = metricsFor(rect, topInset);
        int visibleHeight = metrics.visibleHeight();
        int maxScroll = Math.max(0, computeContentHeight(section) - visibleHeight);
        if (next < 0) {
            return 0;
        }
        if (next > maxScroll) {
            return maxScroll;
        }
        return next;
    }

    private int getScrollOffset(HomePageSection section) {
        return switch (section.kind()) {
            case RECOMMENDED -> recommendedScrollOffset;
            case BOOKMARKS -> bookmarksScrollOffset;
            case HISTORY -> historyScrollOffset;
        };
    }

    private void setScrollOffset(HomePageSection section, int value) {
        switch (section.kind()) {
            case RECOMMENDED -> recommendedScrollOffset = value;
            case BOOKMARKS -> bookmarksScrollOffset = value;
            case HISTORY -> historyScrollOffset = value;
        }
    }

    private void pushScissor(int x, int y, int width, int height) {
        int scale = DisplayScale.scaleFactor();
        Minecraft mc = Minecraft.getMinecraft();
        int sx = x * scale;
        int sy = mc.displayHeight - (y + height) * scale;
        int sw = width * scale;
        int sh = height * scale;
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(sx, Math.max(0, sy), Math.max(0, sw), Math.max(0, sh));
    }

    private void popScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Nullable
    private SectionTarget findSectionTarget(HomePageDataBuilder.HomePageSections sections,
        HomePageLayout.LayoutRects layout, int mouseX, int mouseY) {
        if (contains(layout.recommended(), mouseX, mouseY)) {
            return new SectionTarget(sections.recommended(), layout.recommended(), layout.recommendedTitleSafeTop());
        }
        if (contains(layout.bookmarks(), mouseX, mouseY)) {
            return new SectionTarget(sections.bookmarks(), layout.bookmarks(), 0);
        }
        if (contains(layout.history(), mouseX, mouseY)) {
            return new SectionTarget(sections.history(), layout.history(), 0);
        }
        return null;
    }

    @Nullable
    private HomePageEntry findEntryAt(SectionTarget target, int mouseX, int mouseY) {
        SectionMetrics metrics = metricsFor(target.rect(), target.titleTopInset());
        int rowWidth = Math.max(30, metrics.scrollbarX() - SCROLLBAR_GAP - metrics.contentX());
        if (mouseX < metrics.contentX() || mouseX >= metrics.contentX() + rowWidth) {
            return null;
        }
        int contentY = metrics.contentY();
        int localY = mouseY - contentY + getScrollOffset(target.section());
        if (localY < 0) {
            return null;
        }
        int stride = ROW_HEIGHT + ROW_GAP;
        int index = localY / stride;
        if (index < 0 || index >= target.section()
            .entries()
            .size()) {
            return null;
        }
        int rowTop = index * stride;
        return localY < rowTop + ROW_HEIGHT ? target.section()
            .entries()
            .get(index) : null;
    }

    private boolean contains(HomePageLayout.Rect rect, int mouseX, int mouseY) {
        return mouseX >= rect.x() && mouseX < rect.x() + rect.width()
            && mouseY >= rect.y()
            && mouseY < rect.y() + rect.height();
    }

    private boolean isInsideScrollbar(HomePageLayout.Rect rect, SectionMetrics metrics, int mouseX, int mouseY,
        HomePageSection section) {
        int maxScroll = Math.max(0, computeContentHeight(section) - metrics.visibleHeight());
        if (maxScroll <= 0) {
            return false;
        }
        int x = metrics.scrollbarX();
        return mouseX >= x && mouseX < x + SCROLLBAR_WIDTH
            && mouseY >= metrics.contentY()
            && mouseY < metrics.contentY() + metrics.visibleHeight();
    }

    private int scrollbarThumbOffset(HomePageLayout.Rect rect, SectionMetrics metrics, int mouseY,
        HomePageSection section) {
        int maxScroll = Math.max(0, computeContentHeight(section) - metrics.visibleHeight());
        int thumbHeight = Math.max(
            18,
            metrics.visibleHeight() * metrics.visibleHeight()
                / Math.max(metrics.visibleHeight(), computeContentHeight(section)));
        int travel = Math.max(1, metrics.visibleHeight() - thumbHeight);
        int thumbY = metrics.contentY() + (int) ((long) getScrollOffset(section) * travel / Math.max(1, maxScroll));
        if (mouseY >= thumbY && mouseY < thumbY + thumbHeight) {
            return mouseY - thumbY;
        }
        return thumbHeight / 2;
    }

    private int resolveScrollbarScrollOffset(HomePageSection section, HomePageLayout.Rect rect, int topInset,
        int mouseY, int grabOffset) {
        SectionMetrics metrics = metricsFor(rect, topInset);
        int contentHeight = computeContentHeight(section);
        int maxScroll = Math.max(0, contentHeight - metrics.visibleHeight());
        if (maxScroll <= 0) {
            return 0;
        }
        int thumbHeight = Math.max(
            18,
            metrics.visibleHeight() * metrics.visibleHeight() / Math.max(metrics.visibleHeight(), contentHeight));
        int travel = Math.max(1, metrics.visibleHeight() - thumbHeight);
        int targetThumbY = mouseY - grabOffset;
        int relative = targetThumbY - metrics.contentY();
        if (relative < 0) {
            relative = 0;
        } else if (relative > travel) {
            relative = travel;
        }
        return (int) ((long) relative * maxScroll / travel);
    }

    private SectionMetrics metricsFor(HomePageLayout.Rect rect, int topInset) {
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        int titleY = rect.y() + SECTION_PADDING + topInset;
        int contentX = rect.x() + SECTION_PADDING;
        int contentY = titleY + TITLE_GAP + font.FONT_HEIGHT;
        int scrollbarX = rect.x() + rect.width() - SECTION_PADDING - SCROLLBAR_WIDTH;
        int visibleHeight = Math.max(20, rect.y() + rect.height() - SECTION_PADDING - contentY);
        return new SectionMetrics(titleY, contentX, contentY, scrollbarX, visibleHeight);
    }

    private void drawCenteredEmptyMessage(FontRenderer font, String text, HomePageLayout.Rect rect) {
        if (text == null || text.isEmpty()) {
            return;
        }
        int textWidth = font.getStringWidth(text);
        int textX = rect.x() + (rect.width() - textWidth) / 2;
        int textY = rect.y() + (rect.height() - font.FONT_HEIGHT) / 2;
        font.drawString(text, textX, textY, EMPTY_COLOR, false);
    }

    private String trimToWidth(FontRenderer font, String text, int maxWidth) {
        if (text == null || text.isEmpty() || font.getStringWidth(text) <= maxWidth) {
            return text == null ? "" : text;
        }
        int ellipsisWidth = font.getStringWidth("...");
        if (ellipsisWidth >= maxWidth) {
            return font.trimStringToWidth("...", maxWidth);
        }
        return font.trimStringToWidth(text, maxWidth - ellipsisWidth) + "...";
    }

    private void drawScaledTrimmedString(FontRenderer font, String text, int x, int y, int maxWidth, int color,
        float scale) {
        if (maxWidth <= 0) {
            return;
        }
        String clipped = trimToWidth(font, text, Math.max(1, Math.round(maxWidth / scale)));
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0f);
        GL11.glScalef(scale, scale, 1f);
        font.drawString(clipped, 0, 0, color, false);
        GL11.glPopMatrix();
    }

    private static class PendingClick {

        private final HomePageEntry entry;
        private final int startMouseX;
        private final int startMouseY;
        private final boolean dragged;

        private PendingClick(HomePageEntry entry, int startMouseX, int startMouseY, boolean dragged) {
            this.entry = entry;
            this.startMouseX = startMouseX;
            this.startMouseY = startMouseY;
            this.dragged = dragged;
        }

        private HomePageEntry entry() {
            return entry;
        }

        private int startMouseX() {
            return startMouseX;
        }

        private int startMouseY() {
            return startMouseY;
        }

        private boolean dragged() {
            return dragged;
        }

        private PendingClick withDragged(boolean dragged) {
            return new PendingClick(entry, startMouseX, startMouseY, dragged);
        }
    }

    private static class DragState {

        private final HomePageSection section;
        private final HomePageLayout.Rect rect;
        private final int titleTopInset;
        private final int lastMouseY;
        private final boolean scrollbar;
        private final int grabOffset;

        private DragState(HomePageSection section, HomePageLayout.Rect rect, int titleTopInset, int lastMouseY,
            boolean scrollbar, int grabOffset) {
            this.section = section;
            this.rect = rect;
            this.titleTopInset = titleTopInset;
            this.lastMouseY = lastMouseY;
            this.scrollbar = scrollbar;
            this.grabOffset = grabOffset;
        }

        private HomePageSection section() {
            return section;
        }

        private HomePageLayout.Rect rect() {
            return rect;
        }

        private int titleTopInset() {
            return titleTopInset;
        }

        private int lastMouseY() {
            return lastMouseY;
        }

        private boolean scrollbar() {
            return scrollbar;
        }

        private int grabOffset() {
            return grabOffset;
        }

        private DragState withLastMouseY(int lastMouseY) {
            return new DragState(section, rect, titleTopInset, lastMouseY, scrollbar, grabOffset);
        }
    }

    private static class SectionTarget {

        private final HomePageSection section;
        private final HomePageLayout.Rect rect;
        private final int titleTopInset;

        private SectionTarget(HomePageSection section, HomePageLayout.Rect rect, int titleTopInset) {
            this.section = section;
            this.rect = rect;
            this.titleTopInset = titleTopInset;
        }

        private HomePageSection section() {
            return section;
        }

        private HomePageLayout.Rect rect() {
            return rect;
        }

        private int titleTopInset() {
            return titleTopInset;
        }
    }

    private static class SectionMetrics {

        private final int titleY;
        private final int contentX;
        private final int contentY;
        private final int scrollbarX;
        private final int visibleHeight;

        private SectionMetrics(int titleY, int contentX, int contentY, int scrollbarX, int visibleHeight) {
            this.titleY = titleY;
            this.contentX = contentX;
            this.contentY = contentY;
            this.scrollbarX = scrollbarX;
            this.visibleHeight = visibleHeight;
        }

        private int titleY() {
            return titleY;
        }

        private int contentX() {
            return contentX;
        }

        private int contentY() {
            return contentY;
        }

        private int scrollbarX() {
            return scrollbarX;
        }

        private int visibleHeight() {
            return visibleHeight;
        }
    }
}
