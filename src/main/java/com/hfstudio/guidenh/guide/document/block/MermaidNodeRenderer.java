package com.hfstudio.guidenh.guide.document.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hfstudio.guidenh.guide.document.LytRect;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidNodeShape;
import com.hfstudio.guidenh.guide.internal.util.GuideStringLines;
import com.hfstudio.guidenh.guide.layout.FontMetrics;
import com.hfstudio.guidenh.guide.layout.LayoutContext;
import com.hfstudio.guidenh.guide.render.RenderContext;
import com.hfstudio.guidenh.guide.style.ResolvedTextStyle;

public final class MermaidNodeRenderer {

    private MermaidNodeRenderer() {}

    public static final int DEFAULT_ACCENT = 0xFF7AA2F7;
    public static final int ACCENT_DANGER = 0xFFF7768E;
    public static final int ACCENT_SUCCESS = 0xFF9ECE6A;
    public static final int ACCENT_WARN = 0xFFE0AF68;
    public static final int ACCENT_MUTED = 0xFF8B949E;
    public static final int ACCENT_CIRCLE = 0xFF7DCFFF;
    public static final int ACCENT_CLOUD = 0xFF73DACA;

    public static final int DEFAULT_BACKGROUND = 0xFF1F2A38;
    public static final int ALT_BACKGROUND = 0xFF111922;

    public static final int BADGE_BACKGROUND = 0x262A3340;
    public static final int BADGE_BORDER = 0x66434C57;

    public record NodeColors(int background, int border, int accent) {}

    public static int resolveAccentColor(List<String> classes, MermaidNodeShape shape) {
        int accent = DEFAULT_ACCENT;
        for (String className : classes) {
            String lower = className.toLowerCase();
            if (lower.contains("danger") || lower.contains("error")
                || lower.contains("urgent")
                || lower.contains("red")) {
                accent = ACCENT_DANGER;
                break;
            }
            if (lower.contains("success") || lower.contains("green") || lower.contains("done")) {
                accent = ACCENT_SUCCESS;
                break;
            }
            if (lower.contains("warn") || lower.contains("yellow") || lower.contains("amber")) {
                accent = ACCENT_WARN;
                break;
            }
            if (lower.contains("muted") || lower.contains("gray") || lower.contains("grey")) {
                accent = ACCENT_MUTED;
            }
        }

        accent = switch (shape) {
            case CIRCLE, DOUBLE_CIRCLE -> ACCENT_CIRCLE;
            case HEXAGON -> ACCENT_WARN;
            case CLOUD -> ACCENT_CLOUD;
            case BANG -> ACCENT_DANGER;
            default -> accent;
        };

        return accent;
    }

    public static NodeColors resolveNodeColors(List<String> classes, MermaidNodeShape shape, boolean isRoot) {
        int accent = resolveAccentColor(classes, shape);
        int background = isRoot ? DEFAULT_BACKGROUND : ALT_BACKGROUND;
        return new NodeColors(background, accent, accent);
    }

    public static void renderAccentBar(RenderContext context, LytRect boxRect, int accentColor) {
        context.fillRect(new LytRect(boxRect.x(), boxRect.y(), 3, boxRect.height()), accentColor);
    }

    public static ResolvedTextStyle scaleTextStyle(ResolvedTextStyle base, float zoom) {
        return new ResolvedTextStyle(
            base.fontScale() * zoom,
            base.bold(),
            base.italic(),
            base.underlined(),
            base.wavyUnderline(),
            base.dottedUnderline(),
            base.strikethrough(),
            base.obfuscated(),
            base.font(),
            base.color(),
            base.whiteSpace(),
            base.alignment(),
            base.dropShadow(),
            base.backgroundColor(),
            base.inlineCode());
    }

    public static ResolvedTextStyle getOrScaleStyle(Map<ResolvedTextStyle, ResolvedTextStyle> cache,
        ResolvedTextStyle base, float zoom) {
        return cache.computeIfAbsent(base, key -> scaleTextStyle(key, zoom));
    }

    public static String simplifyIcon(String icon) {
        if (icon == null || icon.trim()
            .isEmpty()) {
            return null;
        }

        String trimmed = icon.trim();
        String leaf = trimmed.substring(lastWhitespaceSeparatedTokenStart(trimmed));
        if (leaf.startsWith("fa-")) {
            leaf = leaf.substring(3);
        }
        leaf = leaf.replace('-', ' ')
            .trim();
        return leaf.isEmpty() ? trimmed : leaf;
    }

    private static int lastWhitespaceSeparatedTokenStart(String text) {
        int index = text.length() - 1;
        while (index >= 0 && !Character.isWhitespace(text.charAt(index))) {
            index--;
        }
        return index + 1;
    }

    @FunctionalInterface
    public interface AdvanceFunction {

        float getAdvance(int codePoint, ResolvedTextStyle style);
    }

    public static int measureText(LayoutContext context, ResolvedTextStyle style, String text) {
        return measureTextInternal(style, text, context::getAdvance);
    }

    public static int measureText(RenderContext context, ResolvedTextStyle style, String text) {
        return context.getStringWidth(text, style);
    }

    public static int measureTextInternal(ResolvedTextStyle style, String text, AdvanceFunction advance) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        float width = 0f;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            width += advance.getAdvance(codePoint, style);
            offset += Character.charCount(codePoint);
        }
        return Math.round(width);
    }

    // ---- Text wrapping ----

    public static List<String> wrapText(RenderContext context, ResolvedTextStyle style, String text, int maxWidth) {
        return wrapText(new LayoutContext(new FontMetrics() {

            @Override
            public float getAdvance(int codePoint, ResolvedTextStyle s) {
                return context.getStringWidth(new String(Character.toChars(codePoint)), s);
            }

            @Override
            public int getLineHeight(ResolvedTextStyle s) {
                return context.getLineHeight(s);
            }
        }), style, text, maxWidth);
    }

    public static List<String> wrapText(LayoutContext context, ResolvedTextStyle style, String text, int maxWidth) {
        List<String> result = new ArrayList<>();
        GuideStringLines.visitLines(text != null ? text : "", (paragraph, lineIndex) -> {
            if (paragraph.isEmpty()) {
                result.add("");
                return true;
            }

            StringBuilder line = new StringBuilder();
            scanWords(paragraph, word -> appendWrappedWord(result, line, context, style, word, maxWidth));
            if (!line.isEmpty()) {
                result.add(line.toString());
            }
            return true;
        });
        return result;
    }

    private static boolean appendWrappedWord(List<String> result, StringBuilder line, LayoutContext context,
        ResolvedTextStyle style, String word, int maxWidth) {
        if (line.isEmpty()) {
            if (measureText(context, style, word) <= maxWidth) {
                line.append(word);
            } else {
                appendBrokenWord(result, line, context, style, word, maxWidth);
            }
            return true;
        }

        String candidate = line + " " + word;
        if (measureText(context, style, candidate) <= maxWidth) {
            line.append(' ')
                .append(word);
            return true;
        }

        result.add(line.toString());
        line.setLength(0);
        if (measureText(context, style, word) <= maxWidth) {
            line.append(word);
        } else {
            appendBrokenWord(result, line, context, style, word, maxWidth);
        }
        return true;
    }

    public static void scanWords(String text, WordVisitor visitor) {
        int start = -1;
        for (int index = 0, length = text.length(); index <= length; index++) {
            char value = index < length ? text.charAt(index) : ' ';
            if (Character.isWhitespace(value)) {
                if (start >= 0) {
                    if (!visitor.accept(text.substring(start, index))) {
                        return;
                    }
                    start = -1;
                }
            } else if (start < 0) {
                start = index;
            }
        }
    }

    private static void appendBrokenWord(List<String> result, StringBuilder line, LayoutContext context,
        ResolvedTextStyle style, String word, int maxWidth) {
        StringBuilder fragment = new StringBuilder();
        for (int offset = 0; offset < word.length();) {
            int codePoint = word.codePointAt(offset);
            String next = fragment + new String(Character.toChars(codePoint));
            if (!fragment.isEmpty() && measureText(context, style, next) > maxWidth) {
                result.add(fragment.toString());
                fragment.setLength(0);
            }
            fragment.appendCodePoint(codePoint);
            offset += Character.charCount(codePoint);
        }
        if (!fragment.isEmpty()) {
            line.append(fragment);
        }
    }

    @FunctionalInterface
    public interface WordVisitor {

        boolean accept(String word);
    }
}
