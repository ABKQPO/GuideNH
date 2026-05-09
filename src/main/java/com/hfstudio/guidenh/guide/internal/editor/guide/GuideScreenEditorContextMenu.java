package com.hfstudio.guidenh.guide.internal.editor.guide;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorPopupLayout;
import com.hfstudio.guidenh.guide.internal.screen.GuideIconButton;

public final class GuideScreenEditorContextMenu {

    public static final int ITEM_HEIGHT = 14;
    private static final int PADDING_X = 6;
    private static final int PADDING_Y = 4;
    private static final int ICON_SIZE = 10;
    private static final int ICON_TEXT_GAP = 4;
    private static final int SUBMENU_GAP = 3;
    private static final int BACKGROUND_COLOR = 0xF0181C22;
    private static final int BORDER_COLOR = 0xFF4D5661;
    private static final int HOVER_COLOR = 0xCC2A3A46;
    private static final int TEXT_COLOR = 0xFFF0F0F0;
    private static final int SEPARATOR_COLOR = 0xFF33404C;

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
    private boolean open;
    private int rootX;
    private int rootY;
    private int rootWidth;
    private int rootHeight;
    private int subX;
    private int subY;
    private int subWidth;
    private int subHeight;
    private int hoveredRootIndex = -1;
    private int hoveredSubIndex = -1;

    public GuideScreenEditorContextMenu(List<Entry> entries) {
        this.entries = entries != null ? Collections.unmodifiableList(new ArrayList<>(entries))
            : Collections.<Entry>emptyList();
    }

    public boolean isOpen() {
        return open;
    }

    public void open(int mouseX, int mouseY, int viewportWidth, int viewportHeight, FontRenderer fontRenderer) {
        rootWidth = computeMenuWidth(entries, fontRenderer);
        rootHeight = computeMenuHeight(entries);
        var rootRect = SceneEditorPopupLayout
            .clampToViewport(mouseX, mouseY, rootWidth, rootHeight, viewportWidth, viewportHeight, 2);
        rootX = rootRect.x();
        rootY = rootRect.y();
        open = true;
        hoveredRootIndex = -1;
        hoveredSubIndex = -1;
        update(mouseX, mouseY, viewportWidth, viewportHeight, fontRenderer);
    }

    public void close() {
        open = false;
        hoveredRootIndex = -1;
        hoveredSubIndex = -1;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button, Listener listener, FontRenderer fontRenderer,
        int viewportWidth, int viewportHeight) {
        if (!open) {
            return false;
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

    public void update(int mouseX, int mouseY, int viewportWidth, int viewportHeight, FontRenderer fontRenderer) {
        if (!open) {
            return;
        }
        int currentRoot = findEntryIndex(mouseX, mouseY, rootX, rootY, rootWidth, rootHeight, entries);
        boolean mouseOverRoot = currentRoot >= 0;
        if (mouseOverRoot) {
            hoveredRootIndex = currentRoot;
        }
        Entry rootEntry = hoveredRootIndex >= 0 && hoveredRootIndex < entries.size() ? entries.get(hoveredRootIndex)
            : null;

        if (rootEntry != null && rootEntry.hasChildren()) {
            subWidth = computeMenuWidth(rootEntry.getChildren(), fontRenderer);
            subHeight = computeMenuHeight(rootEntry.getChildren());
            int preferredSubX = rootX + rootWidth + SUBMENU_GAP;
            if (preferredSubX + subWidth > viewportWidth - 2) {
                preferredSubX = rootX - SUBMENU_GAP - subWidth;
            }
            int preferredSubY = rootY + hoveredRootIndex * ITEM_HEIGHT;
            var submenuRect = SceneEditorPopupLayout
                .clampToViewport(preferredSubX, preferredSubY, subWidth, subHeight, viewportWidth, viewportHeight, 2);
            subX = submenuRect.x();
            subY = submenuRect.y();
            int currentSub = findEntryIndex(mouseX, mouseY, subX, subY, subWidth, subHeight, rootEntry.getChildren());
            hoveredSubIndex = currentSub;
            if (!mouseOverRoot && !contains(mouseX, mouseY, subX, subY, subWidth, subHeight)) {
                hoveredSubIndex = -1;
                hoveredRootIndex = -1;
            }
            return;
        }

        if (!mouseOverRoot) {
            hoveredRootIndex = -1;
        }
        hoveredSubIndex = -1;
    }

    @Nullable
    public String getHoveredTooltip() {
        Entry hovered = getHoveredEntry();
        return hovered != null ? hovered.getLabel() : null;
    }

    private Entry getHoveredEntry() {
        if (hoveredSubIndex >= 0 && hoveredRootIndex >= 0 && hoveredRootIndex < entries.size()) {
            Entry rootEntry = entries.get(hoveredRootIndex);
            if (rootEntry.hasChildren() && hoveredSubIndex < rootEntry.getChildren()
                .size()) {
                return rootEntry.getChildren()
                    .get(hoveredSubIndex);
            }
        }
        if (hoveredRootIndex >= 0 && hoveredRootIndex < entries.size()) {
            return entries.get(hoveredRootIndex);
        }
        return null;
    }

    public void draw(FontRenderer fontRenderer, int mouseX, int mouseY) {
        if (!open) {
            return;
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        drawMenu(minecraft, fontRenderer, rootX, rootY, rootWidth, rootHeight, entries, hoveredRootIndex);
        if (hoveredRootIndex >= 0 && hoveredRootIndex < entries.size()) {
            Entry rootEntry = entries.get(hoveredRootIndex);
            if (rootEntry.hasChildren()) {
                drawMenu(
                    minecraft,
                    fontRenderer,
                    subX,
                    subY,
                    subWidth,
                    subHeight,
                    rootEntry.getChildren(),
                    hoveredSubIndex);
            }
        }
    }

    private void drawMenu(Minecraft minecraft, FontRenderer fontRenderer, int x, int y, int width, int height,
        List<Entry> itemEntries, int hoveredIndex) {
        Gui.drawRect(x, y, x + width, y + height, BACKGROUND_COLOR);
        Gui.drawRect(x, y, x + width, y + 1, BORDER_COLOR);
        Gui.drawRect(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        Gui.drawRect(x, y, x + 1, y + height, BORDER_COLOR);
        Gui.drawRect(x + width - 1, y, x + width, y + height, BORDER_COLOR);

        int drawY = y + PADDING_Y;
        int itemIndex = 0;
        for (Entry entry : itemEntries) {
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
                drawEntryIcon(minecraft, entry, x + PADDING_X, drawY + (ITEM_HEIGHT - ICON_SIZE) / 2);
                fontRenderer.drawString(entry.getLabel(), textX, drawY + 2, TEXT_COLOR);
                if (entry.hasChildren()) {
                    fontRenderer.drawString(
                        ">",
                        x + width - PADDING_X - fontRenderer.getStringWidth(">"),
                        drawY + 2,
                        TEXT_COLOR);
                }
            }
            itemIndex++;
            drawY += ITEM_HEIGHT;
        }
    }

    private void drawEntryIcon(Minecraft minecraft, Entry entry, int x, int y) {
        GuideScreenEditorAction action = entry.getAction();
        if (action == null) {
            return;
        }
        GuideIconButton.drawIcon(minecraft, action.toRole(), x, y, ICON_SIZE, ICON_SIZE, 0xD8FFFFFF);
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

    private int computeMenuHeight(List<Entry> itemEntries) {
        return Math.max(ITEM_HEIGHT, itemEntries.size() * ITEM_HEIGHT + PADDING_Y * 2);
    }

    private int findEntryIndex(int mouseX, int mouseY, int x, int y, int width, int height, List<Entry> itemEntries) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return -1;
        }
        int localY = mouseY - y - PADDING_Y;
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
}
