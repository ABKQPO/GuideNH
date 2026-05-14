package com.hfstudio.guidenh.guide.internal.editor.autocomplete.provider;

import net.minecraft.client.gui.FontRenderer;

public interface AutocompleteCandidate {

    String displayText();

    String replacementText();

    default int renderHeight() {
        return 14;
    }

    /** Width hint for popup sizing. Default 0 means use displayText width. */
    default int renderWidth(FontRenderer fontRenderer) {
        return 0;
    }

    void render(FontRenderer fontRenderer, int x, int y, int width, boolean hovered);
}
