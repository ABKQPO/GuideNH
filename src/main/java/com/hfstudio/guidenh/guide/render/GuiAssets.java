package com.hfstudio.guidenh.guide.render;

import net.minecraft.util.ResourceLocation;

public class GuiAssets {

    public static final GuiSprite MISSING_TEXTURE = new GuiSprite(
        new ResourceLocation("guidenh", "textures/gui/missing.png"),
        0,
        0,
        16,
        16);
    public static final GuiSprite SLOT = new GuiSprite(
        new ResourceLocation("minecraft", "textures/gui/container/inventory.png"),
        7,
        7,
        18,
        18);
    public static final GuiSprite LARGE_SLOT = new GuiSprite(
        new ResourceLocation("minecraft", "textures/gui/container/inventory.png"),
        7,
        7,
        26,
        26);

    public static final GuiAssets INSTANCE = new GuiAssets();

    private GuiAssets() {}

    public static GuiAssets get() {
        return INSTANCE;
    }

    public ResourceLocation getButtonBackground() {
        return new ResourceLocation("guidenh", "textures/gui/button.png");
    }

    public ResourceLocation getScrollBackground() {
        return new ResourceLocation("guidenh", "textures/gui/scroll.png");
    }
}
