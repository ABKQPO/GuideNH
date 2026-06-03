package com.hfstudio.guidenh.guide.internal;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.internal.util.DisplayScale;

public class GuideScreenContextMenu {

    private static final int ITEM_HEIGHT = 14;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int TEXT_Y_OFFSET = 2;
    private static final int MIN_WIDTH = 72;
    private static final int BACKGROUND_COLOR = 0xF0181C22;
    private static final int BORDER_COLOR = 0xFF4D5661;
    private static final int HOVER_COLOR = 0xCC2A3A46;
    private static final int TEXT_COLOR = 0xFFF0F0F0;

    public interface Listener {

        void onAction(ContextMenuAction action);
    }

    public enum ContextMenuAction {
        OPEN_SPECIAL_PAGES,
        CREATE_NEW_PAGE,
        OPEN_CONTAINING_FOLDER
    }

    public static class Entry {

        private final String label;
        private final ContextMenuAction action;

        private Entry(String label, ContextMenuAction action) {
            this.label = label != null ? label : "";
            this.action = action;
        }

        public static Entry action(String label, ContextMenuAction action) {
            return new Entry(label, action);
        }

        public String label() {
            return label;
        }

        public ContextMenuAction action() {
            return action;
        }
    }

    private final List<Entry> entries;
    private boolean open;
    private int x;
    private int y;
    private int width;
    private int height;
    private int hoveredIndex = -1;

    public GuideScreenContextMenu(List<Entry> entries) {
        this.entries = entries != null ? List.copyOf(new ArrayList<>(entries)) : List.of();
    }

    public boolean isOpen() {
        return open;
    }

    public void open(int mouseX, int mouseY, int viewportWidth, int viewportHeight, FontRenderer fontRenderer) {
        width = computeWidth(fontRenderer);
        height = computeHeight();
        x = clampX(mouseX, width, viewportWidth);
        y = clampY(mouseY, height, viewportHeight);
        hoveredIndex = -1;
        open = true;
    }

    public void close() {
        open = false;
        hoveredIndex = -1;
    }

    public void setViewport(int viewportWidth, int viewportHeight, FontRenderer fontRenderer) {
        if (!open) {
            return;
        }
        width = computeWidth(fontRenderer);
        height = computeHeight();
        x = clampX(x, width, viewportWidth);
        y = clampY(y, height, viewportHeight);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button, Listener listener, FontRenderer fontRenderer,
        int viewportWidth, int viewportHeight) {
        if (!open) {
            return false;
        }
        update(mouseX, mouseY, viewportWidth, viewportHeight, fontRenderer);
        if (button != 0) {
            if (!contains(mouseX, mouseY)) {
                close();
                return true;
            }
            return false;
        }
        if (!contains(mouseX, mouseY) || hoveredIndex < 0 || hoveredIndex >= entries.size()) {
            close();
            return true;
        }
        listener.onAction(
            entries.get(hoveredIndex)
                .action());
        close();
        return true;
    }

    public boolean mouseDragged(int mouseX, int mouseY, int button, int viewportWidth, int viewportHeight,
        FontRenderer fontRenderer) {
        if (!open) {
            return false;
        }
        update(mouseX, mouseY, viewportWidth, viewportHeight, fontRenderer);
        return contains(mouseX, mouseY);
    }

    public void mouseReleased(int button) {}

    public void scrollWheel(int mouseX, int mouseY, int wheelDelta, int viewportWidth, int viewportHeight,
        FontRenderer fontRenderer) {}

    public void update(int mouseX, int mouseY, int viewportWidth, int viewportHeight, FontRenderer fontRenderer) {
        if (!open) {
            return;
        }
        hoveredIndex = contains(mouseX, mouseY) ? findEntryIndex(mouseY) : -1;
    }

    public @Nullable String getHoveredTooltip() {
        return hoveredIndex >= 0 && hoveredIndex < entries.size() ? entries.get(hoveredIndex)
            .label() : null;
    }

    public void draw(FontRenderer fontRenderer, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        Gui.drawRect(x, y, x + width, y + height, BACKGROUND_COLOR);
        Gui.drawRect(x, y, x + width, y + 1, BORDER_COLOR);
        Gui.drawRect(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        Gui.drawRect(x, y, x + 1, y + height, BORDER_COLOR);
        Gui.drawRect(x + width - 1, y, x + width, y + height, BORDER_COLOR);

        pushScissor(x + 1, y + 1, width - 2, height - 2);
        try {
            int drawY = y + PADDING_Y;
            for (int index = 0; index < entries.size(); index++) {
                if (index == hoveredIndex) {
                    Gui.drawRect(x + 1, drawY - 1, x + width - 1, drawY + ITEM_HEIGHT - 1, HOVER_COLOR);
                }
                fontRenderer.drawString(
                    entries.get(index)
                        .label(),
                    x + PADDING_X,
                    drawY + TEXT_Y_OFFSET,
                    TEXT_COLOR);
                drawY += ITEM_HEIGHT;
            }
        } finally {
            popScissor();
        }
    }

    private int computeWidth(FontRenderer fontRenderer) {
        int computedWidth = MIN_WIDTH;
        for (Entry entry : entries) {
            computedWidth = Math.max(computedWidth, fontRenderer.getStringWidth(entry.label()) + PADDING_X * 2);
        }
        return computedWidth;
    }

    private int computeHeight() {
        return Math.max(ITEM_HEIGHT + PADDING_Y * 2, entries.size() * ITEM_HEIGHT + PADDING_Y * 2);
    }

    private int clampX(int value, int menuWidth, int viewportWidth) {
        int min = 2;
        int max = Math.max(min, viewportWidth - menuWidth - 2);
        return Math.clamp(value, min, max);
    }

    private int clampY(int value, int menuHeight, int viewportHeight) {
        int min = 2;
        int max = Math.max(min, viewportHeight - menuHeight - 2);
        return Math.clamp(value, min, max);
    }

    private boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int findEntryIndex(int mouseY) {
        int localY = mouseY - y - PADDING_Y;
        int index = localY / ITEM_HEIGHT;
        return index >= 0 && index < entries.size() ? index : -1;
    }

    private void pushScissor(int x, int y, int width, int height) {
        int scale = DisplayScale.scaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            x * scale,
            Minecraft.getMinecraft().displayHeight - (y + height) * scale,
            Math.max(0, width * scale),
            Math.max(0, height * scale));
    }

    private void popScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }
}
