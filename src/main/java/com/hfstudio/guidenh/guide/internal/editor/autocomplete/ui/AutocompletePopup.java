package com.hfstudio.guidenh.guide.internal.editor.autocomplete.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider.AutocompleteCandidate;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorPopupLayout;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;

public class AutocompletePopup {

    public static final int MAX_VISIBLE_ITEMS = 5;
    public static final int PADDING_X = 6;
    public static final int PADDING_Y = 4;
    public static final int SCROLLBAR_W = 5;
    public static final int BACKGROUND_COLOR = 0xF0181C22;
    public static final int BORDER_COLOR = 0xFF4D5661;
    public static final int HOVER_COLOR = 0xCC2A3A46;
    public static final int SCROLLBAR_TRACK_COLOR = 0x35101010;
    public static final int SCROLLBAR_THUMB_COLOR = 0xA0D8D8D8;
    /** Gap between popup and cursor when flipped above (roughly FONT_HEIGHT + cursor gap). */
    private static final int FLIP_GAP = 22;

    private boolean open;
    private List<AutocompleteCandidate> candidates = Collections.emptyList();
    private List<String> lastCandidateKeys = Collections.emptyList();
    private int selectedIndex;
    private int scrollY;
    private int x, y, width, height;
    private int viewportWidth, viewportHeight;

    public boolean isOpen() {
        return open;
    }

    public void show(List<AutocompleteCandidate> candidates, int anchorX, int anchorY, int viewportWidth,
        int viewportHeight, FontRenderer fontRenderer) {
        this.candidates = candidates != null ? candidates : Collections.emptyList();

        if (this.candidates.isEmpty()) {
            open = false;
            return;
        }

        // Preserve scroll/selection when candidate list hasn't changed
        boolean sameList = keysMatch(this.candidates, lastCandidateKeys);
        if (!sameList) {
            this.selectedIndex = 0;
            this.scrollY = 0;
            lastCandidateKeys = candidateKeys(this.candidates);
            computeSize(fontRenderer);
        }
        this.viewportWidth = viewportWidth;
        this.viewportHeight = viewportHeight;

        placePopup(anchorX, anchorY, viewportWidth, viewportHeight);
        if (selectedIndex >= this.candidates.size()) selectedIndex = 0;
        this.open = true;
    }

    private static List<String> candidateKeys(List<AutocompleteCandidate> list) {
        List<String> keys = new ArrayList<>(list.size());
        for (AutocompleteCandidate c : list) keys.add(c.displayText());
        return keys;
    }

    private static boolean keysMatch(List<AutocompleteCandidate> list, List<String> keys) {
        if (list.size() != keys.size()) return false;
        for (int i = 0; i < list.size(); i++) {
            if (!list.get(i)
                .displayText()
                .equals(keys.get(i))) return false;
        }
        return true;
    }

    public void close() {
        open = false;
        candidates = Collections.emptyList();
        lastCandidateKeys = Collections.emptyList();
        selectedIndex = -1;
    }

    public void moveSelection(int delta) {
        if (!open || candidates.isEmpty()) return;
        int next = selectedIndex + delta;
        if (next < 0) next = 0;
        if (next >= candidates.size()) next = candidates.size() - 1;
        selectedIndex = next;
        ensureSelectionVisible();
    }

    @Nullable
    public AutocompleteCandidate getSelected() {
        if (!open || selectedIndex < 0 || selectedIndex >= candidates.size()) return null;
        return candidates.get(selectedIndex);
    }

    public void scrollWheel(int delta) {
        if (!open) return;
        int maxH = computeMaxItemHeight();
        scrollY = clampScroll(scrollY - delta * maxH * 2);
    }

    /** Recompute popup position without changing candidates or selection. */
    public void reposition(int anchorX, int anchorY, int viewportWidth, int viewportHeight, FontRenderer fontRenderer) {
        if (!open) return;
        placePopup(anchorX, anchorY, viewportWidth, viewportHeight);
        this.scrollY = clampScroll(scrollY);
    }

    /** Place popup below the anchor; flip above if it doesn't fit. */
    private void placePopup(int anchorX, int anchorY, int viewportWidth, int viewportHeight) {
        // Try below cursor first
        LytRect below = SceneEditorPopupLayout
            .clampToViewport(anchorX, anchorY, width, height, viewportWidth, viewportHeight, 2);
        if (below.y() >= anchorY - 2) {
            this.x = below.x();
            this.y = below.y();
            return;
        }
        // Not enough room below �?flip above cursor
        int aboveY = anchorY - height - FLIP_GAP;
        LytRect above = SceneEditorPopupLayout
            .clampToViewport(anchorX, aboveY, width, height, viewportWidth, viewportHeight, 2);
        this.x = above.x();
        this.y = above.y();
    }

    public boolean mouseClicked(int mouseX, int mouseY) {
        if (!open || !contains(mouseX, mouseY)) {
            return false;
        }
        if (isInsideScrollbar(mouseX, mouseY)) {
            int contentHeight = computeContentHeight();
            if (contentHeight > height) {
                int trackH = Math.max(1, height - 2);
                int maxScroll = Math.max(0, contentHeight - height);
                scrollY = (mouseY - y) * maxScroll / trackH;
                scrollY = clampScroll(scrollY);
            }
            return true;
        }
        int index = findItemIndex(mouseX, mouseY);
        if (index >= 0 && index < candidates.size()) {
            selectedIndex = index;
        }
        return true;
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public void draw(FontRenderer fontRenderer, int mouseX, int mouseY) {
        if (!open) return;

        Gui.drawRect(x - 1, y - 1, x + width + 1, y + height + 1, BORDER_COLOR);
        Gui.drawRect(x, y, x + width, y + height, BACKGROUND_COLOR);

        pushScissor(x + 1, y + 1, width - 2, height - 2);

        int drawY = y + PADDING_Y - scrollY;
        int itemIndex = 0;
        int itemHeight = computeMaxItemHeight();
        for (AutocompleteCandidate candidate : candidates) {
            if (drawY + itemHeight >= y + 1 && drawY <= y + height - 1) {
                if (itemIndex == selectedIndex) {
                    Gui.drawRect(x + 1, drawY - 1, x + width - SCROLLBAR_W - 1, drawY + itemHeight - 1, HOVER_COLOR);
                }
                candidate.render(
                    fontRenderer,
                    x + PADDING_X,
                    drawY,
                    width - PADDING_X * 2 - (hasScrollbar() ? SCROLLBAR_W : 0),
                    itemIndex == selectedIndex);
            }
            itemIndex++;
            drawY += itemHeight;
        }

        popScissor();
        drawScrollbar();
    }

    private void computeSize(FontRenderer fontRenderer) {
        int maxW = 72;
        int maxItemH = 14;
        for (AutocompleteCandidate c : candidates) {
            int textW = fontRenderer.getStringWidth(c.displayText()) + PADDING_X * 2;
            int renderW = c.renderWidth(fontRenderer);
            int w = Math.max(textW, renderW > 0 ? renderW + PADDING_X * 2 : 0);

            if (w > maxW) maxW = w;
            if (c.renderHeight() > maxItemH) maxItemH = c.renderHeight();
        }
        this.width = Math.max(72, maxW + SCROLLBAR_W);
        int visibleItems = Math.min(candidates.size(), MAX_VISIBLE_ITEMS);
        this.height = visibleItems * maxItemH + PADDING_Y * 2;
    }

    private int computeMaxItemHeight() {
        int max = 14;
        for (AutocompleteCandidate c : candidates) {
            if (c.renderHeight() > max) max = c.renderHeight();
        }
        return max;
    }

    private int computeContentHeight() {
        int h = 0;
        int maxH = computeMaxItemHeight();
        for (int i = 0; i < candidates.size(); i++) {
            h += maxH;
        }
        return h + PADDING_Y * 2;
    }

    private boolean hasScrollbar() {
        return candidates.size() > MAX_VISIBLE_ITEMS;
    }

    private void ensureSelectionVisible() {
        int itemH = computeMaxItemHeight();
        int selTop = selectedIndex * itemH;
        int selBottom = selTop + itemH;
        int viewH = height - PADDING_Y * 2;
        if (selTop < scrollY) {
            scrollY = selTop;
        } else if (selBottom > scrollY + viewH) {
            scrollY = selBottom - viewH;
        }
        scrollY = clampScroll(scrollY);
    }

    private int findItemIndex(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) return -1;
        int itemH = computeMaxItemHeight();
        int localY = mouseY - y - PADDING_Y + scrollY;
        int index = localY / itemH;
        if (index < 0 || index >= candidates.size()) return -1;
        return index;
    }

    private void drawScrollbar() {
        int contentH = computeContentHeight();
        if (contentH <= height) return;
        int barX = x + width - SCROLLBAR_W;
        Gui.drawRect(barX, y + 1, x + width - 1, y + height - 1, SCROLLBAR_TRACK_COLOR);
        int thumbH = Math.max(16, height * height / Math.max(1, contentH));
        int maxScroll = Math.max(0, contentH - height);
        int thumbY = y + (maxScroll > 0 ? (height - thumbH) * scrollY / maxScroll : 0);
        Gui.drawRect(barX, thumbY, x + width - 1, thumbY + thumbH, SCROLLBAR_THUMB_COLOR);
    }

    private boolean isInsideScrollbar(int mouseX, int mouseY) {
        if (!hasScrollbar()) return false;
        return mouseX >= x + width - SCROLLBAR_W && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int clampScroll(int value) {
        int maxScroll = Math.max(0, computeContentHeight() - height);
        if (value < 0) return 0;
        return Math.min(value, maxScroll);
    }

    private static void pushScissor(int x, int y, int width, int height) {
        Minecraft mc = Minecraft.getMinecraft();
        int scale = DisplayScale.scaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            x * scale,
            mc.displayHeight - (y + height) * scale,
            Math.max(0, width * scale),
            Math.max(0, height * scale));
    }

    private static void popScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
