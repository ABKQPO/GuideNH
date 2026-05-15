package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

public class TextCandidate implements AutocompleteCandidate {

    private final String text;
    private static final int TEXT_COLOR = 0xFFF0F0F0;

    public TextCandidate(String text) {
        this.text = text;
    }

    @Override
    public String displayText() {
        return text;
    }

    @Override
    public String replacementText() {
        return text;
    }

    @Override
    public void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered) {
        fontRenderer.drawString(text, x, y + 2, TEXT_COLOR);
    }
}
