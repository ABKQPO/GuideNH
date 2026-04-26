package com.hfstudio.guidenh.guide.internal.editor.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.client.gui.FontRenderer;

import com.github.bsideup.jabel.Desugar;

public final class SceneEditorMultilineTextLayoutCache {

    private final List<VisualLine> visualLines = new ArrayList<>();
    private List<VisualLine> readonlyVisualLines = Collections.emptyList();
    private int contentWidthPixels;
    private int contentHeightPixels;

    public void rebuild(String text, FontRenderer fontRenderer, int textWidth, boolean wrapEnabled, int lineHeight) {
        visualLines.clear();
        contentWidthPixels = 0;
        contentHeightPixels = 0;

        String safeText = text != null ? text : "";
        int safeTextWidth = Math.max(1, textWidth);
        int lineStart = 0;
        for (int i = 0; i <= safeText.length(); i++) {
            boolean atEnd = i == safeText.length();
            if (!atEnd && safeText.charAt(i) != '\n') {
                continue;
            }

            String logicalLine = safeText.substring(lineStart, i);
            boolean endsWithNewline = !atEnd;
            appendLogicalLine(logicalLine, lineStart, endsWithNewline, fontRenderer, safeTextWidth, wrapEnabled);
            lineStart = i + 1;
        }

        if (visualLines.isEmpty()) {
            visualLines.add(new VisualLine(0, 0, "", false));
        }

        readonlyVisualLines = Collections.unmodifiableList(new ArrayList<>(visualLines));
        contentHeightPixels = visualLines.size() * Math.max(0, lineHeight);
    }

    public List<VisualLine> getVisualLines() {
        return readonlyVisualLines;
    }

    public int getContentWidthPixels() {
        return contentWidthPixels;
    }

    public int getContentHeightPixels() {
        return contentHeightPixels;
    }

    private void appendLogicalLine(String logicalLine, int lineStart, boolean endsWithNewline,
        FontRenderer fontRenderer, int textWidth, boolean wrapEnabled) {
        if (!wrapEnabled) {
            visualLines.add(new VisualLine(lineStart, lineStart + logicalLine.length(), logicalLine, endsWithNewline));
            contentWidthPixels = Math.max(contentWidthPixels, fontRenderer.getStringWidth(logicalLine));
            return;
        }

        if (logicalLine.isEmpty()) {
            visualLines.add(new VisualLine(lineStart, lineStart, "", endsWithNewline));
            return;
        }

        int offset = 0;
        while (offset < logicalLine.length()) {
            String remaining = logicalLine.substring(offset);
            String chunk = fontRenderer.trimStringToWidth(remaining, textWidth);
            if (chunk.isEmpty()) {
                chunk = remaining.substring(0, 1);
            }
            int consumed = Math.max(1, chunk.length());
            int startIndex = lineStart + offset;
            int endIndex = startIndex + consumed;
            boolean lineBreak = endsWithNewline && endIndex == lineStart + logicalLine.length();
            visualLines.add(new VisualLine(startIndex, endIndex, chunk, lineBreak));
            contentWidthPixels = Math.max(contentWidthPixels, fontRenderer.getStringWidth(chunk));
            offset += consumed;
        }
    }

    @Desugar
    public record VisualLine(int startIndex, int endIndex, String text, boolean endsWithNewline) {}
}
