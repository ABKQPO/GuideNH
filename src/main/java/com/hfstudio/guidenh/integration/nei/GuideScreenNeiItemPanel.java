package com.hfstudio.guidenh.integration.nei;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.guide.internal.editor.gui.SceneEditorMultilineTextArea;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.nei.GuideNeiItemReferenceFormatter.RecipeQueryKind;

import codechicken.nei.ItemList;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.api.ItemInfo;
import codechicken.nei.guihook.GuiContainerManager;
import cpw.mods.fml.common.FMLLog;

public class GuideScreenNeiItemPanel {

    private static final int SLOT_SIZE = 18;
    private static final int PANEL_PADDING = 4;
    private static final int MIN_COLUMNS = 1;
    private static final int MAX_COLUMNS = 8;
    private static final int MIN_VISIBLE_ROWS = 2;
    private static final int DRAG_THRESHOLD_PIXELS = 3;
    private static final int BACKGROUND_COLOR = 0xD0181A20;
    private static final int BORDER_COLOR = 0xFF545A66;
    private static final int HOVER_COLOR = 0x66FFFFFF;
    private static final int DRAG_TARGET_COLOR = 0x6684C8FF;

    private final Minecraft minecraft;
    private final EditorAccess editorAccess;
    private final List<ItemStack> visibleItems = new ArrayList<>();

    private int x;
    private int y;
    private int width;
    private int height;
    private int columns = MIN_COLUMNS;
    private int visibleRows = MIN_VISIBLE_ROWS;
    private int scrollRow;
    private boolean activeLastFrame;
    private boolean itemListDirty = true;
    @Nullable
    private ItemStack mouseDownStack;
    @Nullable
    private ItemStack draggedStack;
    private int mouseDownX;
    private int mouseDownY;

    public GuideScreenNeiItemPanel(Minecraft minecraft, EditorAccess editorAccess) {
        this.minecraft = minecraft;
        this.editorAccess = editorAccess;
    }

    public void draw(int mouseX, int mouseY) {
        if (!isActive()) {
            resetInteraction();
            activeLastFrame = false;
            return;
        }
        activeLastFrame = true;
        layout();
        rebuildVisibleItemsIfNeeded();
        clampScroll();
        drawBackground();
        drawItems(mouseX, mouseY);
        drawDraggedStack(mouseX, mouseY);
    }

    public boolean drawTooltip(int mouseX, int mouseY) {
        if (!isActive()) {
            return false;
        }
        ItemStack stack = hoveredStack(mouseX, mouseY);
        if (stack == null) {
            return false;
        }
        List<String> lines = tooltipLines(stack);
        if (lines.isEmpty()) {
            return false;
        }
        editorAccess.drawTooltip(lines, mouseX, mouseY);
        return true;
    }

    public boolean keyTyped(char typedChar, int keyCode) {
        if (!isEnabledByMods() || !isConfiguredForEditor()) {
            return false;
        }
        boolean hideKeyDown = isNeiKeyDown("gui.hide");
        if (hideKeyDown) {
            NEIClientConfig.toggleBooleanSetting("inventory.hidden");
            resetInteraction();
            return true;
        }
        if (!activeLastFrame || NEIClientConfig.isHidden() || !isActive()) {
            return false;
        }
        ItemStack stack = hoveredStack(editorAccess.mouseX(), editorAccess.mouseY());
        if (stack == null) {
            return false;
        }
        if (isNeiKeyDown("gui.recipe")) {
            return insertRecipeTag(stack, RecipeQueryKind.RECIPE);
        }
        if (isNeiKeyDown("gui.usage")) {
            return insertRecipeTag(stack, RecipeQueryKind.USAGE);
        }
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!isActive() || button != 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        ItemStack stack = hoveredStack(mouseX, mouseY);
        if (stack == null) {
            return true;
        }
        mouseDownStack = stack.copy();
        mouseDownX = mouseX;
        mouseDownY = mouseY;
        return true;
    }

    public boolean mouseDragged(int mouseX, int mouseY, int button) {
        if (!isActive() || button != 0 || mouseDownStack == null) {
            return false;
        }
        if (draggedStack == null && movedPastDragThreshold(mouseX, mouseY)) {
            draggedStack = mouseDownStack.copy();
        }
        return true;
    }

    public boolean mouseReleased(int mouseX, int mouseY, int state) {
        if (!isActive() || mouseDownStack == null || state == -1) {
            resetInteraction();
            return false;
        }
        ItemStack releasedStack = draggedStack != null ? draggedStack : mouseDownStack;
        boolean handled = false;
        if (draggedStack != null && editorAccess.canDropIntoEditor(mouseX, mouseY)) {
            String itemReference = GuideNeiItemReferenceFormatter.formatItemReference(releasedStack);
            if (itemReference != null) {
                editorAccess.insertAtMouse(itemReference, mouseX, mouseY);
                handled = true;
            }
        }
        resetInteraction();
        return handled;
    }

    public boolean mouseWheel(int mouseX, int mouseY, int wheelDelta) {
        if (!isActive() || wheelDelta == 0 || !contains(mouseX, mouseY)) {
            return false;
        }
        int rowDelta = wheelDelta > 0 ? -1 : 1;
        scrollRow += rowDelta;
        clampScroll();
        return true;
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        return isActive() && contains(mouseX, mouseY);
    }

    public boolean isDraggingItem() {
        return draggedStack != null;
    }

    public void markItemListDirty() {
        itemListDirty = true;
    }

    private boolean insertRecipeTag(ItemStack stack, RecipeQueryKind queryKind) {
        String recipeTag = GuideNeiItemReferenceFormatter.formatRecipeTag(stack, queryKind);
        if (recipeTag == null) {
            return false;
        }
        editorAccess.insertAtSelection(recipeTag);
        return true;
    }

    private boolean isActive() {
        return isEnabledByMods() && isConfiguredForEditor() && !NEIClientConfig.isHidden();
    }

    private boolean isConfiguredForEditor() {
        return editorAccess.isEditorActive() && !editorAccess.isFullWidth()
            && ModConfig.ui.guideEditorNeiItemPanelOutsideWindow;
    }

    private boolean isEnabledByMods() {
        return Mods.NotEnoughItems.isModLoaded() && NEIClientConfig.isEnabled() && NEIClientConfig.isLoaded();
    }

    private void layout() {
        int availableRight = editorAccess.screenWidth() - editorAccess.panelRight() - PANEL_PADDING;
        columns = clamp(availableRight / SLOT_SIZE, MIN_COLUMNS, MAX_COLUMNS);
        width = columns * SLOT_SIZE + PANEL_PADDING * 2;
        x = Math.min(editorAccess.screenWidth() - width - 1, editorAccess.panelRight() + PANEL_PADDING);
        if (x < editorAccess.panelRight() + 1) {
            x = editorAccess.panelRight() + 1;
        }
        y = Math.max(PANEL_PADDING, editorAccess.panelTop());
        height = Math.max(0, editorAccess.panelBottom() - y);
        visibleRows = Math.max(MIN_VISIBLE_ROWS, (height - PANEL_PADDING * 2) / SLOT_SIZE);
        height = visibleRows * SLOT_SIZE + PANEL_PADDING * 2;
    }

    private void rebuildVisibleItemsIfNeeded() {
        if (!itemListDirty && !visibleItems.isEmpty()) {
            return;
        }
        itemListDirty = false;
        visibleItems.clear();
        List<ItemStack> source = ItemList.items != null ? ItemList.items : Collections.<ItemStack>emptyList();
        for (int i = 0, count = source.size(); i < count; i++) {
            ItemStack stack = source.get(i);
            if (stack == null || stack.getItem() == null || ItemInfo.isHidden(stack)) {
                continue;
            }
            visibleItems.add(stack);
        }
    }

    private void drawBackground() {
        Gui.drawRect(x, y, x + width, y + height, BACKGROUND_COLOR);
        Gui.drawRect(x, y, x + width, y + 1, BORDER_COLOR);
        Gui.drawRect(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        Gui.drawRect(x, y, x + 1, y + height, BORDER_COLOR);
        Gui.drawRect(x + width - 1, y, x + width, y + height, BORDER_COLOR);
    }

    private void drawItems(int mouseX, int mouseY) {
        int startIndex = scrollRow * columns;
        int endIndex = Math.min(visibleItems.size(), startIndex + columns * visibleRows);
        for (int index = startIndex; index < endIndex; index++) {
            ItemStack stack = visibleItems.get(index);
            int slot = index - startIndex;
            int slotX = x + PANEL_PADDING + slot % columns * SLOT_SIZE;
            int slotY = y + PANEL_PADDING + slot / columns * SLOT_SIZE;
            if (isSlotHovered(mouseX, mouseY, slotX, slotY)) {
                Gui.drawRect(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, HOVER_COLOR);
            }
            drawStack(stack, slotX + 1, slotY + 1);
        }
        if (draggedStack != null && editorAccess.canDropIntoEditor(mouseX, mouseY)) {
            SceneEditorMultilineTextArea textArea = editorAccess.textArea();
            if (textArea != null) {
                Gui.drawRect(mouseX - 1, mouseY - 6, mouseX + 1, mouseY + 7, DRAG_TARGET_COLOR);
            }
        }
    }

    private void drawDraggedStack(int mouseX, int mouseY) {
        if (draggedStack == null) {
            return;
        }
        GL11.glPushMatrix();
        GuiContainerManager.drawItems.zLevel += 100F;
        try {
            drawStack(draggedStack, mouseX - 8, mouseY - 8);
        } finally {
            GuiContainerManager.drawItems.zLevel -= 100F;
            GL11.glPopMatrix();
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }

    private void drawStack(ItemStack stack, int drawX, int drawY) {
        try {
            RenderHelper.enableGUIStandardItemLighting();
            GuiContainerManager.drawItem(drawX, drawY, stack, true, "");
        } catch (Throwable t) {
            FMLLog.getLogger()
                .debug("[GuideNH] Failed to render NEI editor stack {}", stack, t);
        } finally {
            RenderHelper.disableStandardItemLighting();
        }
    }

    @Nullable
    private ItemStack hoveredStack(int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) {
            return null;
        }
        int localX = mouseX - x - PANEL_PADDING;
        int localY = mouseY - y - PANEL_PADDING;
        if (localX < 0 || localY < 0) {
            return null;
        }
        int column = localX / SLOT_SIZE;
        int row = localY / SLOT_SIZE;
        if (column < 0 || column >= columns || row < 0 || row >= visibleRows) {
            return null;
        }
        int index = (scrollRow + row) * columns + column;
        if (index < 0 || index >= visibleItems.size()) {
            return null;
        }
        ItemStack stack = visibleItems.get(index);
        return stack != null ? stack.copy() : null;
    }

    private List<String> tooltipLines(ItemStack stack) {
        try {
            List<String> lines = GuiContainerManager.itemDisplayNameMultiline(stack, null, true);
            lines.add(EnumChatFormatting.DARK_GRAY + "R: RecipeFor");
            lines.add(EnumChatFormatting.DARK_GRAY + "U: RecipeUsage");
            return lines;
        } catch (Throwable t) {
            List<String> fallback = new ArrayList<>();
            try {
                fallback.add(stack.getRarity().rarityColor + stack.getDisplayName());
            } catch (Throwable ignored) {
                fallback.add(EnumChatFormatting.WHITE + "Unnamed");
            }
            String itemReference = GuideNeiItemReferenceFormatter.formatItemReference(stack);
            if (itemReference != null) {
                fallback.add(EnumChatFormatting.GRAY + itemReference);
            }
            fallback.add(EnumChatFormatting.DARK_GRAY + "R: RecipeFor");
            fallback.add(EnumChatFormatting.DARK_GRAY + "U: RecipeUsage");
            return fallback;
        }
    }

    private boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private boolean isSlotHovered(int mouseX, int mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX < slotX + SLOT_SIZE && mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
    }

    private boolean movedPastDragThreshold(int mouseX, int mouseY) {
        return Math.abs(mouseX - mouseDownX) >= DRAG_THRESHOLD_PIXELS
            || Math.abs(mouseY - mouseDownY) >= DRAG_THRESHOLD_PIXELS;
    }

    private void clampScroll() {
        int maxRow = Math.max(0, (visibleItems.size() + columns - 1) / columns - visibleRows);
        if (scrollRow < 0) {
            scrollRow = 0;
        }
        if (scrollRow > maxRow) {
            scrollRow = maxRow;
        }
    }

    private boolean isNeiKeyDown(String key) {
        try {
            return NEIClientConfig.isKeyHashDown(key);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private void resetInteraction() {
        mouseDownStack = null;
        draggedStack = null;
        mouseDownX = 0;
        mouseDownY = 0;
    }

    private int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    public interface EditorAccess {

        boolean isEditorActive();

        boolean isFullWidth();

        int screenWidth();

        int panelRight();

        int panelTop();

        int panelBottom();

        int mouseX();

        int mouseY();

        @Nullable
        SceneEditorMultilineTextArea textArea();

        boolean canDropIntoEditor(int mouseX, int mouseY);

        void insertAtMouse(String text, int mouseX, int mouseY);

        void insertAtSelection(String text);

        void drawTooltip(List<String> lines, int mouseX, int mouseY);
    }
}
