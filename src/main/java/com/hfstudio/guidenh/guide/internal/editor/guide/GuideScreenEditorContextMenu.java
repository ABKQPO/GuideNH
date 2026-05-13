package com.hfstudio.guidenh.guide.internal.editor.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorMultilineTextArea;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorPopupLayout;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;
import com.hfstudio.guidenh.guide.internal.util.DisplayScale;

public final class GuideScreenEditorContextMenu {

    public static final int ITEM_HEIGHT = 14;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int ICON_SIZE = 10;
    private static final int ICON_TEXT_GAP = 4;
    private static final int TEXT_Y_OFFSET = 2;
    private static final int TEXT_VISUAL_HEIGHT = 9;
    private static final int SCROLLBAR_W = SceneEditorMultilineTextArea.SCROLLBAR_SIZE;
    private static final int BACKGROUND_COLOR = 0xF0181C22;
    private static final int BORDER_COLOR = 0xFF4D5661;
    private static final int HOVER_COLOR = 0xCC2A3A46;
    private static final int TEXT_COLOR = 0xFFF0F0F0;
    private static final int SEPARATOR_COLOR = 0xFF33404C;
    private static final int SCROLLBAR_TRACK_COLOR = 0x35101010;
    private static final int SCROLLBAR_THUMB_COLOR = 0xA0D8D8D8;

    public interface Listener {

        void onAction(GuideScreenEditorAction action);
    }

    public static final class Entry {

        private final String label;
        @Nullable
        private final GuideScreenEditorAction action;
        private final List<Entry> children;
        private final boolean separator;

        private Entry(String label, @Nullable GuideScreenEditorAction action, List<Entry> children, boolean separator) {
            this.label = label != null ? label : "";
            this.action = action;
            this.children = children;
            this.separator = separator;
        }

        public static Entry action(GuideScreenEditorAction action) {
            return new Entry(action.getTooltip(), action, Collections.<Entry>emptyList(), false);
        }

        public static Entry submenu(String label, List<Entry> children) {
            List<Entry> safeChildren = children != null ? new ArrayList<>(children) : new ArrayList<Entry>();
            return new Entry(label, null, Collections.unmodifiableList(safeChildren), false);
        }

        public static Entry separator() {
            return new Entry("", null, Collections.<Entry>emptyList(), true);
        }

        public String getLabel() {
            return label;
        }

        @Nullable
        public GuideScreenEditorAction getAction() {
            return action;
        }

        public List<Entry> getChildren() {
            return children;
        }

        public boolean isSeparator() {
            return separator;
        }

        public boolean isLeaf() {
            return !separator && action != null && children.isEmpty();
        }

        public boolean hasChildren() {
            return !children.isEmpty();
        }
    }

    private final List<Entry> entries;
    private final List<MenuPane> panes = new ArrayList<>();
    private boolean open;
    private int draggingScrollbarPaneIndex = -1;
    private int scrollbarGrabOffset;
    private int activePaneIndex = -1;

    public GuideScreenEditorContextMenu(List<Entry> entries) {
        this.entries = entries != null ? Collections.unmodifiableList(new ArrayList<>(entries))
            : Collections.<Entry>emptyList();
    }

    public boolean isOpen() {
        return open;
    }

    public void open(int mouseX, int mouseY, int viewportWidth, int viewportHeight, FontRenderer fontRenderer) {
        int rootWidth = computeMenuWidth(entries, fontRenderer);
        int rootHeight = clampMenuHeight(computeMenuContentHeight(entries), viewportHeight);
        var rootRect = SceneEditorPopupLayout
            .clampToViewport(mouseX, mouseY, rootWidth, rootHeight, viewportWidth, viewportHeight, 2);
        panes.clear();
        panes.add(new MenuPane(entries, rootRect.x(), rootRect.y(), rootWidth, rootHeight));
        open = true;
        draggingScrollbarPaneIndex = -1;
        update(mouseX, mouseY, viewportWidth, viewportHeight, fontRenderer);
    }

    public void setViewport(int viewportWidth, int viewportHeight, FontRenderer fontRenderer) {
        if (!open || panes.isEmpty()) {
            return;
        }
        MenuPane rootPane = panes.get(0);
        rootPane.width = computeMenuWidth(entries, fontRenderer);
        rootPane.height = clampMenuHeight(computeMenuContentHeight(entries), viewportHeight);
        rootPane.scrollY = clampScroll(rootPane.scrollY, rootPane.entries, rootPane.height);
        var rootRect = SceneEditorPopupLayout
            .clampToViewport(rootPane.x, rootPane.y, rootPane.width, rootPane.height, viewportWidth, viewportHeight, 2);
        rootPane.x = rootRect.x();
        rootPane.y = rootRect.y();
    }

    public void close() {
        open = false;
        panes.clear();
        draggingScrollbarPaneIndex = -1;
        activePaneIndex = -1;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button, Listener listener, FontRenderer fontRenderer,
        int viewportWidth, int viewportHeight) {
        if (!open) {
            return false;
        }
        if (button == 0 && startScrollbarDrag(mouseX, mouseY)) {
            return true;
        }
        update(mouseX, mouseY, viewportWidth, viewportHeight, fontRenderer);
        Entry hovered = getHoveredEntry();
        if (hovered == null) {
            close();
            return true;
        }
        if (hovered.hasChildren()) {
            return true;
        }
        if (button == 0 && hovered.getAction() != null) {
            listener.onAction(hovered.getAction());
        }
        close();
        return true;
    }

    public boolean mouseDragged(int mouseX, int mouseY, int button, int viewportWidth, int viewportHeight,
        FontRenderer fontRenderer) {
        if (!open || button != 0 || draggingScrollbarPaneIndex < 0 || draggingScrollbarPaneIndex >= panes.size()) {
            return false;
        }
        MenuPane pane = panes.get(draggingScrollbarPaneIndex);
        pane.scrollY = scrollFromMouse(mouseY, pane.y, pane.height, computeMenuContentHeight(pane.entries));
        update(mouseX, mouseY, viewportWidth, viewportHeight, fontRenderer);
        return true;
    }

    public void mouseReleased(int button) {
        if (button == 0) {
            draggingScrollbarPaneIndex = -1;
        }
    }

    public void scrollWheel(int mouseX, int mouseY, int wheelDelta, int viewportWidth, int viewportHeight,
        FontRenderer fontRenderer) {
        if (!open || wheelDelta == 0) {
            return;
        }
        update(mouseX, mouseY, viewportWidth, viewportHeight, fontRenderer);
        int step = ITEM_HEIGHT * 2;
        int paneIndex = findDeepestPaneIndex(mouseX, mouseY);
        if (paneIndex < 0 && !panes.isEmpty()) {
            paneIndex = 0;
        }
        if (paneIndex >= 0) {
            MenuPane pane = panes.get(paneIndex);
            pane.scrollY = clampScroll(pane.scrollY - Integer.signum(wheelDelta) * step, pane.entries, pane.height);
        }
        update(mouseX, mouseY, viewportWidth, viewportHeight, fontRenderer);
    }

    public void update(int mouseX, int mouseY, int viewportWidth, int viewportHeight, FontRenderer fontRenderer) {
        if (!open || panes.isEmpty()) {
            return;
        }
        for (MenuPane pane : panes) {
            pane.scrollY = clampScroll(pane.scrollY, pane.entries, pane.height);
        }

        int paneIndex = findDeepestPaneIndex(mouseX, mouseY);
        if (paneIndex < 0) {
            activePaneIndex = -1;
            panes.get(0).hoveredIndex = -1;
            trimPanesAfter(0);
            return;
        }

        MenuPane pane = panes.get(paneIndex);
        int entryIndex = findEntryIndex(
            mouseX,
            mouseY,
            pane.x,
            pane.y,
            pane.width,
            pane.height,
            pane.scrollY,
            pane.entries);
        if (entryIndex < 0) {
            pane.hoveredIndex = -1;
            trimPanesAfter(paneIndex);
            activePaneIndex = -1;
            return;
        }

        activePaneIndex = paneIndex;
        if (pane.hoveredIndex != entryIndex) {
            pane.hoveredIndex = entryIndex;
            trimPanesAfter(paneIndex);
        }
        ensureChildPane(paneIndex, entryIndex, viewportWidth, viewportHeight, fontRenderer);
    }

    private void ensureChildPane(int paneIndex, int entryIndex, int viewportWidth, int viewportHeight,
        FontRenderer fontRenderer) {
        MenuPane parentPane = panes.get(paneIndex);
        Entry parentEntry = parentPane.entries.get(entryIndex);
        if (!parentEntry.hasChildren()) {
            trimPanesAfter(paneIndex);
            return;
        }

        List<Entry> childEntries = parentEntry.getChildren();
        int childWidth = computeMenuWidth(childEntries, fontRenderer);
        int childHeight = clampMenuHeight(computeMenuContentHeight(childEntries), viewportHeight);
        int childX = parentPane.x + parentPane.width - 1;
        if (childX + childWidth > viewportWidth - 2) {
            childX = parentPane.x - childWidth + 1;
        }
        childX = clampToViewportX(childX, childWidth, viewportWidth);
        int childY = parentPane.y + PADDING_Y + entryIndex * ITEM_HEIGHT - parentPane.scrollY;
        childY = clampToViewportY(childY, childHeight, viewportHeight);

        int childPaneIndex = paneIndex + 1;
        if (childPaneIndex < panes.size()) {
            MenuPane childPane = panes.get(childPaneIndex);
            if (childPane.entries != childEntries) {
                childPane.entries = childEntries;
                childPane.hoveredIndex = -1;
                childPane.scrollY = 0;
            }
            childPane.x = childX;
            childPane.y = childY;
            childPane.width = childWidth;
            childPane.height = childHeight;
            childPane.scrollY = clampScroll(childPane.scrollY, childPane.entries, childPane.height);
        } else {
            panes.add(new MenuPane(childEntries, childX, childY, childWidth, childHeight));
        }
    }

    private int clampToViewportX(int x, int width, int viewportWidth) {
        int minX = 2;
        int maxX = Math.max(minX, viewportWidth - width - 2);
        if (x < minX) {
            return minX;
        }
        return Math.min(x, maxX);
    }

    private int clampToViewportY(int y, int height, int viewportHeight) {
        int minY = 2;
        int maxY = Math.max(minY, viewportHeight - height - 2);
        if (y < minY) {
            return minY;
        }
        return Math.min(y, maxY);
    }

    private void trimPanesAfter(int paneIndex) {
        while (panes.size() > paneIndex + 1) {
            panes.remove(panes.size() - 1);
        }
    }

    private int findDeepestPaneIndex(int mouseX, int mouseY) {
        for (int i = panes.size() - 1; i >= 0; i--) {
            MenuPane pane = panes.get(i);
            if (contains(mouseX, mouseY, pane.x, pane.y, pane.width, pane.height)) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    public String getHoveredTooltip() {
        Entry hovered = getHoveredEntry();
        return hovered != null ? hovered.getLabel() : null;
    }

    private Entry getHoveredEntry() {
        if (activePaneIndex < 0 || activePaneIndex >= panes.size()) {
            return null;
        }
        MenuPane pane = panes.get(activePaneIndex);
        if (pane.hoveredIndex >= 0 && pane.hoveredIndex < pane.entries.size()) {
            return pane.entries.get(pane.hoveredIndex);
        }
        return null;
    }

    public void draw(FontRenderer fontRenderer, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        for (MenuPane pane : panes) {
            drawMenu(
                minecraft,
                fontRenderer,
                pane.x,
                pane.y,
                pane.width,
                pane.height,
                pane.entries,
                pane.hoveredIndex,
                pane.scrollY);
        }
    }

    private void drawMenu(Minecraft minecraft, FontRenderer fontRenderer, int x, int y, int width, int height,
        List<Entry> itemEntries, int hoveredIndex, int scrollY) {
        Gui.drawRect(x, y, x + width, y + height, BACKGROUND_COLOR);
        Gui.drawRect(x, y, x + width, y + 1, BORDER_COLOR);
        Gui.drawRect(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        Gui.drawRect(x, y, x + 1, y + height, BORDER_COLOR);
        Gui.drawRect(x + width - 1, y, x + width, y + height, BORDER_COLOR);

        pushScissor(x + 1, y + 1, width - 2, height - 2);
        int drawY = y + PADDING_Y - scrollY;
        int itemIndex = 0;
        for (Entry entry : itemEntries) {
            if (drawY + ITEM_HEIGHT < y + 1) {
                itemIndex++;
                drawY += ITEM_HEIGHT;
                continue;
            }
            if (drawY > y + height - 1) {
                break;
            }
            if (entry.isSeparator()) {
                Gui.drawRect(
                    x + PADDING_X,
                    drawY + ITEM_HEIGHT / 2,
                    x + width - PADDING_X,
                    drawY + ITEM_HEIGHT / 2 + 1,
                    SEPARATOR_COLOR);
            } else {
                if (itemIndex == hoveredIndex) {
                    Gui.drawRect(x + 1, drawY - 1, x + width - 1, drawY + ITEM_HEIGHT - 1, HOVER_COLOR);
                }
                int textX = x + PADDING_X + ICON_SIZE + ICON_TEXT_GAP;
                drawEntryIcon(minecraft, entry, x + PADDING_X, computeIconYForRow(drawY));
                fontRenderer.drawString(entry.getLabel(), textX, drawY + TEXT_Y_OFFSET, TEXT_COLOR);
                if (entry.hasChildren()) {
                    fontRenderer.drawString(
                        ">",
                        x + width - PADDING_X - fontRenderer.getStringWidth(">"),
                        drawY + TEXT_Y_OFFSET,
                        TEXT_COLOR);
                }
            }
            itemIndex++;
            drawY += ITEM_HEIGHT;
        }
        popScissor();
        drawScrollbar(x, y, width, height, itemEntries, scrollY);
    }

    private void drawEntryIcon(Minecraft minecraft, Entry entry, int x, int y) {
        GuideScreenEditorAction action = entry.getAction();
        if (action == null) {
            return;
        }
        GuideIconButton.drawIcon(minecraft, action.toRole(), x, y, ICON_SIZE, ICON_SIZE, 0xD8FFFFFF);
    }

    static int computeIconYForRow(int rowY) {
        int textVisualCenter = rowY + TEXT_Y_OFFSET + TEXT_VISUAL_HEIGHT / 2;
        return textVisualCenter - ICON_SIZE / 2;
    }

    private int computeMenuWidth(List<Entry> itemEntries, FontRenderer fontRenderer) {
        int width = 0;
        for (Entry entry : itemEntries) {
            if (entry.isSeparator()) {
                continue;
            }
            int itemWidth = fontRenderer.getStringWidth(entry.getLabel()) + PADDING_X * 2 + ICON_SIZE + ICON_TEXT_GAP;
            if (entry.hasChildren()) {
                itemWidth += PADDING_X + fontRenderer.getStringWidth(">");
            }
            if (itemWidth > width) {
                width = itemWidth;
            }
        }
        return Math.max(72, width);
    }

    private int computeMenuContentHeight(List<Entry> itemEntries) {
        return Math.max(ITEM_HEIGHT, itemEntries.size() * ITEM_HEIGHT + PADDING_Y * 2);
    }

    private int findEntryIndex(int mouseX, int mouseY, int x, int y, int width, int height, int scrollY,
        List<Entry> itemEntries) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return -1;
        }
        int localY = mouseY - y - PADDING_Y + scrollY;
        int index = localY / ITEM_HEIGHT;
        if (index < 0 || index >= itemEntries.size()) {
            return -1;
        }
        Entry entry = itemEntries.get(index);
        return entry.isSeparator() ? -1 : index;
    }

    private boolean contains(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private int clampMenuHeight(int contentHeight, int viewportHeight) {
        int maxHeight = Math.max(ITEM_HEIGHT + PADDING_Y * 2, viewportHeight - 4);
        return Math.max(ITEM_HEIGHT + PADDING_Y * 2, Math.min(contentHeight, maxHeight));
    }

    private int clampScroll(int scrollY, List<Entry> itemEntries, int height) {
        int maxScroll = Math.max(0, computeMenuContentHeight(itemEntries) - height);
        if (scrollY < 0) {
            return 0;
        }
        return Math.min(scrollY, maxScroll);
    }

    private boolean startScrollbarDrag(int mouseX, int mouseY) {
        for (int i = panes.size() - 1; i >= 0; i--) {
            MenuPane pane = panes.get(i);
            if (isInsideScrollbar(
                mouseX,
                mouseY,
                pane.x,
                pane.y,
                pane.width,
                pane.height,
                pane.entries,
                pane.scrollY)) {
                draggingScrollbarPaneIndex = i;
                scrollbarGrabOffset = mouseY
                    - scrollbarThumbY(pane.y, pane.height, computeMenuContentHeight(pane.entries), pane.scrollY);
                return true;
            }
        }
        return false;
    }

    private boolean isInsideScrollbar(int mouseX, int mouseY, int x, int y, int width, int height,
        List<Entry> itemEntries, int scrollY) {
        if (computeMenuContentHeight(itemEntries) <= height) {
            return false;
        }
        int barX = x + width - SCROLLBAR_W;
        return mouseX >= barX && mouseX < barX + SCROLLBAR_W && mouseY >= y && mouseY < y + height;
    }

    private int scrollFromMouse(int mouseY, int y, int height, int contentHeight) {
        int maxScroll = Math.max(0, contentHeight - height);
        if (maxScroll <= 0) {
            return 0;
        }
        int thumbH = scrollbarThumbHeight(height, contentHeight);
        int track = Math.max(1, height - thumbH);
        int rel = mouseY - scrollbarGrabOffset - y;
        if (rel < 0) rel = 0;
        if (rel > track) rel = track;
        return (int) ((long) rel * maxScroll / track);
    }

    private void drawScrollbar(int x, int y, int width, int height, List<Entry> itemEntries, int scrollY) {
        int contentHeight = computeMenuContentHeight(itemEntries);
        if (contentHeight <= height) {
            return;
        }
        int barX = x + width - SCROLLBAR_W;
        Gui.drawRect(barX, y + 1, x + width - 1, y + height - 1, SCROLLBAR_TRACK_COLOR);
        int thumbY = scrollbarThumbY(y, height, contentHeight, scrollY);
        int thumbH = scrollbarThumbHeight(height, contentHeight);
        Gui.drawRect(barX, thumbY, x + width - 1, thumbY + thumbH, SCROLLBAR_THUMB_COLOR);
    }

    private int scrollbarThumbY(int y, int height, int contentHeight, int scrollY) {
        int thumbH = scrollbarThumbHeight(height, contentHeight);
        int maxScroll = Math.max(0, contentHeight - height);
        return maxScroll > 0 ? y + (int) ((long) (height - thumbH) * scrollY / maxScroll) : y;
    }

    private int scrollbarThumbHeight(int height, int contentHeight) {
        return Math.max(16, (int) ((long) height * height / Math.max(1, contentHeight)));
    }

    private void pushScissor(int x, int y, int width, int height) {
        Minecraft minecraft = Minecraft.getMinecraft();
        int scale = DisplayScale.scaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
            x * scale,
            minecraft.displayHeight - (y + height) * scale,
            Math.max(0, width * scale),
            Math.max(0, height * scale));
    }

    private void popScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1f, 1f, 1f, 1f);
    }

    private static final class MenuPane {

        private List<Entry> entries;
        private int x;
        private int y;
        private int width;
        private int height;
        private int scrollY;
        private int hoveredIndex = -1;

        private MenuPane(List<Entry> entries, int x, int y, int width, int height) {
            this.entries = entries;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
