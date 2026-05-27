package com.hfstudio.guidenh.mixins.early.forge;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = GuiIngameForge.class, remap = false)
public interface AccessorGuiIngameForge {

    @Accessor("res")
    ScaledResolution getScaledResolution();

    @Accessor("res")
    void setScaledResolution(ScaledResolution scaledResolution);

    @Accessor("fontrenderer")
    FontRenderer getFontRenderer();

    @Accessor("fontrenderer")
    void setFontRenderer(FontRenderer fontRenderer);

    @Accessor("eventParent")
    RenderGameOverlayEvent getEventParent();

    @Accessor("eventParent")
    void setEventParent(RenderGameOverlayEvent eventParent);

    @Invoker("renderHUDText")
    void invokeRenderHudText(int width, int height);
}
