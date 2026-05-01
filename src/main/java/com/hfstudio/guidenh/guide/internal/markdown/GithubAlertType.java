package com.hfstudio.guidenh.guide.internal.markdown;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.guide.internal.GuidebookText;

public enum GithubAlertType {

    NOTE(GuidebookText.MarkdownAlertNote, "\u24d8", new ConstantColor(0xFF638EF1)),
    TIP(GuidebookText.MarkdownAlertTip, "\u2726", new ConstantColor(0xFF61B75D)),
    IMPORTANT(GuidebookText.MarkdownAlertImportant, "\u203b", new ConstantColor(0xFF8755DD)),
    WARNING(GuidebookText.MarkdownAlertWarning, "\u26a0", new ConstantColor(0xFFC79D3E)),
    CAUTION(GuidebookText.MarkdownAlertCaution, "\u2622", new ConstantColor(0xFFE46150));

    private final GuidebookText label;
    private final String symbol;
    private final ConstantColor accentColor;

    GithubAlertType(GuidebookText label, String symbol, ConstantColor accentColor) {
        this.label = label;
        this.symbol = symbol;
        this.accentColor = accentColor;
    }

    public String displayText() {
        return label.text();
    }

    public String symbol() {
        return symbol;
    }

    public ConstantColor accentColor() {
        return accentColor;
    }

    public static @Nullable GithubAlertType fromDirective(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.trim()
            .toUpperCase();
        if (normalized.startsWith("[!NOTE]") || normalized.startsWith("!NOTE") || normalized.startsWith("NOTE")) {
            return NOTE;
        }
        if (normalized.startsWith("[!TIP]") || normalized.startsWith("!TIP") || normalized.startsWith("TIP")) {
            return TIP;
        }
        if (normalized.startsWith("[!IMPORTANT]") || normalized.startsWith("!IMPORTANT")
            || normalized.startsWith("IMPORTANT")) {
            return IMPORTANT;
        }
        if (normalized.startsWith("[!WARNING]") || normalized.startsWith("!WARNING")
            || normalized.startsWith("WARNING")) {
            return WARNING;
        }
        if (normalized.startsWith("[!CAUTION]") || normalized.startsWith("!CAUTION")
            || normalized.startsWith("CAUTION")) {
            return CAUTION;
        }
        return null;
    }
}
