package com.hfstudio.guidenh.guide.render;

import net.minecraft.util.ResourceLocation;

public class GuiSprite {

    private final ResourceLocation texture;
    private final int u;
    private final int v;
    private final int width;
    private final int height;
    private final int texWidth;
    private final int texHeight;

    public GuiSprite(ResourceLocation texture, int u, int v, int width, int height, int texWidth, int texHeight) {
        this.texture = texture;
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    public GuiSprite(ResourceLocation texture, int u, int v, int width, int height) {
        this(texture, u, v, width, height, 256, 256);
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public int getU() {
        return u;
    }

    public int getV() {
        return v;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getTexWidth() {
        return texWidth;
    }

    public int getTexHeight() {
        return texHeight;
    }
}
