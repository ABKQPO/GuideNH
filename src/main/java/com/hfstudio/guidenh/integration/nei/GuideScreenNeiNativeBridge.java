package com.hfstudio.guidenh.integration.nei;

import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.hfstudio.guidenh.config.ModConfig;
import com.hfstudio.guidenh.integration.Mods;
import com.hfstudio.guidenh.integration.api.GuideNhIntegrationRegistry;
import com.hfstudio.guidenh.integration.nei.GuideScreenNeiBridge.EditorAccess;

import codechicken.nei.ItemPanels;
import codechicken.nei.NEIClientConfig;
import codechicken.nei.PositionedStack;
import codechicken.nei.api.IGuiContainerOverlay;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.GuiRecipe;
import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.GuiUsageRecipe;
import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class GuideScreenNeiNativeBridge {

    private static final int MIN_LEFT_PANEL_WIDTH = 104;
    private static final int MIN_RIGHT_PANEL_WIDTH = 126;
    private static final int BOTTOM_PANEL_HEIGHT = 24;

    protected GuideScreenNeiNativeBridge() {}

    public static boolean isNativeEditorNeiEnabled(EditorAccess editorAccess) {
        return isEnabledByMods() && isConfiguredForEditor(editorAccess);
    }

    public static int reservedSidePixels(EditorAccess editorAccess) {
        if (!isNativeEditorNeiEnabled(editorAccess)) {
            return 0;
        }
        return Math.max(MIN_LEFT_PANEL_WIDTH, MIN_RIGHT_PANEL_WIDTH);
    }

    public static int reservedBottomPixels(EditorAccess editorAccess) {
        return isNativeEditorNeiEnabled(editorAccess) ? BOTTOM_PANEL_HEIGHT : 0;
    }

    public static boolean handleItemDrop(EditorAccess editorAccess, int mouseX, int mouseY) {
        if (!isNativeEditorNeiEnabled(editorAccess) || !editorAccess.canDropIntoEditor(mouseX, mouseY)) {
            return false;
        }
        ItemStack draggedStack = ItemPanels.itemPanel.draggedStack;
        if (draggedStack == null) {
            return false;
        }
        String itemReference = GuideNeiItemReferenceFormatter.formatItemReference(draggedStack);
        if (itemReference == null) {
            return false;
        }
        editorAccess.insertAtMouse(
            formatEditorDropText(editorAccess, draggedStack, itemReference, mouseX, mouseY),
            mouseX,
            mouseY);
        ItemPanels.itemPanel.draggedStack = null;
        return true;
    }

    private static String formatEditorDropText(EditorAccess editorAccess, ItemStack stack, String itemReference,
        int mouseX, int mouseY) {
        if (!editorAccess.canInsertRichTagAtMouse(mouseX, mouseY)) {
            return itemReference;
        }
        String tagName = isBlockStack(stack) ? "BlockImage" : "ItemImage";
        return "<" + tagName + " id=\"" + GuideNeiItemReferenceFormatter.escapeAttribute(itemReference) + "\" />";
    }

    private static boolean isBlockStack(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        Block block = Block.getBlockFromItem(stack.getItem());
        return block != null && block != Block.getBlockFromName("air");
    }

    public static boolean isNeiMouseOver(EditorAccess editorAccess, int mouseX, int mouseY) {
        GuiContainerManager manager = nativeManager(editorAccess);
        return manager != null && isNeiMouseOver(manager, mouseX, mouseY);
    }

    public static boolean isDraggingItem() {
        if (!isEnabledByMods()) {
            return false;
        }
        return ItemPanels.itemPanel.draggedStack != null;
    }

    public static boolean mouseClicked(EditorAccess editorAccess, int mouseX, int mouseY, int button) {
        GuiContainerManager manager = nativeManager(editorAccess);
        if (manager == null) {
            return false;
        }
        editorAccess.prepareForTemporaryScreenChange();
        try {
            return manager.mouseClicked(mouseX, mouseY, button);
        } finally {
            if (Minecraft.getMinecraft().currentScreen == editorAccess.container()) {
                editorAccess.cancelTemporaryScreenChange();
            }
        }
    }

    public static boolean mouseDragged(EditorAccess editorAccess, int mouseX, int mouseY, int button, long heldTime) {
        GuiContainerManager manager = nativeManager(editorAccess);
        if (manager == null) {
            return false;
        }
        manager.mouseDragged(mouseX, mouseY, button, heldTime);
        return isDraggingItem() || isNeiMouseOver(manager, mouseX, mouseY);
    }

    public static boolean mouseReleased(EditorAccess editorAccess, int mouseX, int mouseY, int button) {
        GuiContainerManager manager = nativeManager(editorAccess);
        if (manager == null) {
            return false;
        }
        editorAccess.prepareForTemporaryScreenChange();
        try {
            if (manager.overrideMouseUp(mouseX, mouseY, button)) {
                return true;
            }
            manager.mouseUp(mouseX, mouseY, button);
            return isNeiMouseOver(manager, mouseX, mouseY);
        } finally {
            if (Minecraft.getMinecraft().currentScreen == editorAccess.container()) {
                editorAccess.cancelTemporaryScreenChange();
            }
        }
    }

    public static boolean mouseScrolled(EditorAccess editorAccess, int mouseX, int mouseY, int wheelDelta) {
        GuiContainerManager manager = nativeManager(editorAccess);
        if (manager == null || !isNeiMouseOver(manager, mouseX, mouseY)) {
            return false;
        }
        manager.mouseScrolled(Integer.signum(wheelDelta));
        return true;
    }

    public static boolean keyTyped(EditorAccess editorAccess, char typedChar, int keyCode) {
        if (isEditorTextFocused(editorAccess)) {
            return false;
        }
        GuiContainerManager manager = configuredManager(editorAccess);
        if (manager == null) {
            return false;
        }
        editorAccess.prepareForTemporaryScreenChange();
        boolean handled = false;
        try {
            handled = manager.firstKeyTyped(typedChar, keyCode);
            if (!handled) {
                handled = manager.lastKeyTyped(keyCode, typedChar);
            }
            return handled;
        } finally {
            if (Minecraft.getMinecraft().currentScreen == editorAccess.container()) {
                editorAccess.cancelTemporaryScreenChange();
            }
        }
    }

    private static boolean isEditorTextFocused(EditorAccess editorAccess) {
        return editorAccess.textArea() != null && editorAccess.textArea()
            .isFocused();
    }

    public static void tick(EditorAccess editorAccess) {
        GuiContainerManager manager = configuredManager(editorAccess);
        if (manager != null) {
            manager.updateScreen();
        }
    }

    public static void drawNativeNei(EditorAccess editorAccess, int mouseX, int mouseY) {
        GuiContainerManager manager = nativeManager(editorAccess);
        if (manager == null) {
            return;
        }
        manager.preDraw();
        GL11.glPushMatrix();
        try {
            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
            GL11.glEnable(GL11.GL_LIGHTING);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glTranslatef(editorAccess.containerLeft(), editorAccess.containerTop(), 0.0F);
            manager.renderObjects(mouseX, mouseY);
        } finally {
            GL11.glPopMatrix();
            RenderHelper.disableStandardItemLighting();
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void drawNativeNeiTooltip(EditorAccess editorAccess, int mouseX, int mouseY) {
        GuiContainerManager manager = nativeManager(editorAccess);
        if (manager != null) {
            manager.renderToolTips(mouseX, mouseY);
        }
    }

    public static void init() {
        if (Mods.NotEnoughItems.isModLoaded()) {
            MinecraftForge.EVENT_BUS.register(new GuideScreenNeiBridgeEvents());
        }
    }

    private static boolean isConfiguredForEditor(EditorAccess editorAccess) {
        return editorAccess.isEditorActive() && !editorAccess.isFullWidth()
            && ModConfig.ui.guideEditorNeiItemPanelOutsideWindow;
    }

    private static boolean isEnabledByMods() {
        return Mods.NotEnoughItems.isModLoaded() && NEIClientConfig.isEnabled() && NEIClientConfig.isLoaded();
    }

    private static @Nullable GuiContainerManager nativeManager(EditorAccess editorAccess) {
        if (!isNativeEditorNeiEnabled(editorAccess) || NEIClientConfig.isHidden()) {
            return null;
        }
        return configuredManager(editorAccess);
    }

    private static @Nullable GuiContainerManager configuredManager(EditorAccess editorAccess) {
        if (!isNativeEditorNeiEnabled(editorAccess)) {
            return null;
        }
        return GuiContainerManager.getManager(editorAccess.container());
    }

    private static boolean isNeiMouseOver(GuiContainerManager manager, int mouseX, int mouseY) {
        return manager.objectUnderMouse(mouseX, mouseY);
    }

    public static class GuideScreenNeiBridgeEvents {

        @SubscribeEvent
        public void onRecipeButtons(GuiRecipeButton.UpdateRecipeButtonsEvent.Post event) {
            EditorAccess editorAccess = editorAccessFor((GuiScreen) event.gui);
            if (editorAccess == null) {
                return;
            }
            for (int i = 0; i < event.buttonList.size(); i++) {
                GuiRecipeButton button = event.buttonList.get(i);
                if (button instanceof GuiOverlayButton) {
                    event.buttonList.set(i, new InsertRecipeButton(editorAccess, button.handlerRef, button));
                }
            }
        }
    }

    public static @Nullable EditorAccess editorAccessFor(@Nullable GuiScreen screen) {
        if (screen instanceof EditorAccess access && isNativeEditorNeiEnabled(access)) {
            return access;
        }
        if (screen instanceof GuiRecipe<?>recipe) {
            GuiScreen first = recipe.getFirstScreenGeneral();
            if (first instanceof EditorAccess access && isNativeEditorNeiEnabled(access)) {
                return access;
            }
        }
        if (screen instanceof IGuiContainerOverlay overlay) {
            GuiScreen first = overlay.getFirstScreenGeneral();
            if (first instanceof EditorAccess access && isNativeEditorNeiEnabled(access)) {
                return access;
            }
        }
        return null;
    }

    public static class InsertRecipeButton extends GuiRecipeButton {

        private final EditorAccess editorAccess;
        private final GuiRecipeButton source;

        public InsertRecipeButton(EditorAccess editorAccess, RecipeHandlerRef handlerRef, GuiRecipeButton source) {
            super(handlerRef, source.xPosition, source.yPosition, source.id, source.displayString);
            this.editorAccess = editorAccess;
            this.source = source;
        }

        @Override
        public List<String> handleTooltip(List<String> currenttip) {
            return source.handleTooltip(currenttip);
        }

        @Override
        public Map<String, String> handleHotkeys(int mousex, int mousey, Map<String, String> hotkeys) {
            return source.handleHotkeys(mousex, mousey, hotkeys);
        }

        @Override
        public void lastKeyTyped(char keyChar, int keyID) {
            source.lastKeyTyped(keyChar, keyID);
        }

        @Override
        public void drawItemOverlay() {
            source.drawItemOverlay();
        }

        @Override
        public void update() {
            source.update();
        }

        @Override
        public boolean mouseScrolled(int scroll) {
            return source.mouseScrolled(scroll);
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY) {
            source.xPosition = xPosition;
            source.yPosition = yPosition;
            source.width = width;
            source.height = height;
            source.enabled = enabled;
            source.visible = visible;
            source.drawButton(minecraft, mouseX, mouseY);
        }

        @Override
        public void mouseReleased(int mouseX, int mouseY) {
            boolean usageRecipe = isSourceUsageRecipe();
            String recipeTag = formatRecipeTag(handlerRef.handler, handlerRef.recipeIndex, usageRecipe);
            if (recipeTag != null) {
                editorAccess.returnToEditorScreen();
                editorAccess.insertAtSelection(recipeTag);
            }
        }

        private boolean isSourceUsageRecipe() {
            return Minecraft.getMinecraft().currentScreen instanceof GuiUsageRecipe;
        }

        private @Nullable String formatRecipeTag(IRecipeHandler handler, int recipeIndex, boolean usageRecipe) {
            ItemStack stack = usageRecipe ? safeIngredientStack(handler, recipeIndex)
                : safeResultStack(handler, recipeIndex);
            if (stack == null) {
                return null;
            }
            String itemReference = GuideNeiItemReferenceFormatter.formatItemReference(stack);
            if (itemReference == null) {
                return null;
            }
            String handlerId = GuideNhIntegrationRegistry.global()
                .lookupRecipeHandlerId(handler);
            String handlerName = GuideNhIntegrationRegistry.global()
                .lookupRecipeHandlerName(handler);
            StringBuilder builder = new StringBuilder("<").append(usageRecipe ? "RecipeUsage" : "RecipeFor")
                .append(" id=\"")
                .append(GuideNeiItemReferenceFormatter.escapeAttribute(itemReference))
                .append('"');
            if (!handlerId.isEmpty()) {
                builder.append(" handlerId=\"")
                    .append(GuideNeiItemReferenceFormatter.escapeAttribute(handlerId))
                    .append('"');
            } else if (!handlerName.isEmpty()) {
                builder.append(" handlerName=\"")
                    .append(GuideNeiItemReferenceFormatter.escapeAttribute(handlerName))
                    .append('"');
            }
            builder.append(" recipeIndex=\"")
                .append(recipeIndex)
                .append("\" />");
            return builder.toString();
        }

        private @Nullable ItemStack safeIngredientStack(IRecipeHandler handler, int recipeIndex) {
            try {
                List<PositionedStack> ingredients = handler.getIngredientStacks(recipeIndex);
                if (ingredients == null) {
                    return null;
                }
                for (PositionedStack ingredient : ingredients) {
                    if (ingredient != null && ingredient.item != null) {
                        return ingredient.item.copy();
                    }
                }
                return null;
            } catch (Throwable ignored) {
                return null;
            }
        }

        private @Nullable ItemStack safeResultStack(IRecipeHandler handler, int recipeIndex) {
            try {
                PositionedStack result = handler.getResultStack(recipeIndex);
                return result != null && result.item != null ? result.item.copy() : null;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

}
