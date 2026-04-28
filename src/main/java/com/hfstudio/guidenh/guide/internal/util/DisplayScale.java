package com.hfstudio.guidenh.guide.internal.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

public class DisplayScale {

    private static int cachedDW = -1;
    private static int cachedDH = -1;
    private static int cachedGuiScale = -1;
    private static int cachedScaleFactor = 2;
    private static int cachedScaledWidth = 0;
    private static int cachedScaledHeight = 0;

    private DisplayScale() {}

    private static void refreshIfNeeded() {
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
