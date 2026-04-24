package com.hfstudio.guidenh.guide.document.interaction;

public class TextTooltip implements GuideTooltip {

    private final String text;

    public TextTooltip(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }
}
