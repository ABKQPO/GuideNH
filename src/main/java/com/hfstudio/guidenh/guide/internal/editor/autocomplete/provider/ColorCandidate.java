package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;

import com.hfstudio.guidenh.guide.render.GuideTextRenderer;

public class ColorCandidate implements AutocompleteCandidate {

    private final String name;
    private final int color;
    private static final int SWATCH_SIZE = 12;
    private static final int TEXT_X = SWATCH_SIZE + 6;
    private static final int TEXT_COLOR = 0xFFF0F0F0;

    public ColorCandidate(String name, int color) {
        this.name = name;
        this.color = color;
    }

    @Override
    public String displayText() {
        return name;
    }

    @Override
    public String replacementText() {
        return name;
    }

    @Override
    public int renderHeight() {
        return 16;
    }

    @Override
    public int renderWidth(FontRenderer fontRenderer) {
        return SWATCH_SIZE + 6 + GuideTextRenderer.getStringWidth(fontRenderer, name) + 6;
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        // Draw color swatch
        int swatchY = y + (renderHeight() - SWATCH_SIZE) / 2;
        Gui.drawRect(x, swatchY, x + SWATCH_SIZE, swatchY + SWATCH_SIZE, 0xFF000000 | color);
        Gui.drawRect(x - 1, swatchY - 1, x + SWATCH_SIZE + 1, swatchY + SWATCH_SIZE + 1, 0xFF4D5661);
        // Draw name
        GuideTextRenderer.drawString(fontRenderer, name, x + TEXT_X, y + 3, TEXT_COLOR);
    }
}
