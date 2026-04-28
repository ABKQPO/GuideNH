package com.hfstudio.guidenh.guide.internal.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class DisplayScale {

    public static int cachedDW = -1;
    public static int cachedDH = -1;
    public static int cachedGuiScale = -1;
    public static int cachedScaleFactor = 2;
    public static int cachedScaledWidth = 0;
    public static int cachedScaledHeight = 0;

    private DisplayScale() {}

    public static void refreshIfNeeded() {
        Minecraft mc = Minecraft.getMinecraft();
        int dw = mc.displayWidth;
        int dh = mc.displayHeight;
        int gs = mc.gameSettings != null ? mc.gameSettings.guiScale : 0;
        if (dw == cachedDW && dh == cachedDH && gs == cachedGuiScale) return;
        ScaledResolution sr = new ScaledResolution(mc, dw, dh);
        cachedDW = dw;
        cachedDH = dh;
        cachedGuiScale = gs;
        cachedScaleFactor = sr.getScaleFactor();
        cachedScaledWidth = sr.getScaledWidth();
        cachedScaledHeight = sr.getScaledHeight();
    }

    public static int scaleFactor() {
        refreshIfNeeded();
        return cachedScaleFactor;
    }

    public static int scaledWidth() {
        refreshIfNeeded();
        return cachedScaledWidth;
    }

    public static int scaledHeight() {
        refreshIfNeeded();
        return cachedScaledHeight;
    }
}
