package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.guide.color.ColorValue;
import com.hfstudio.guidenh.guide.color.ConstantColor;
import com.hfstudio.guidenh.libs.mdast.model.MdAstAnyContent;
import com.hfstudio.guidenh.libs.mdast.model.MdAstBlockquote;
import com.hfstudio.guidenh.libs.mdast.model.MdAstInlineCode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParagraph;
import com.hfstudio.guidenh.libs.mdast.model.MdAstText;

public final class MarkdownRuntimeBlocks {

    private MarkdownRuntimeBlocks() {}

    public static @Nullable GithubAlertBlock extractGithubAlert(MdAstBlockquote blockquote) {
        BlockquoteDirective directive = parseBlockquoteDirective(blockquote);
        if (directive == null || directive.alertType() == null) {
            return null;
        }
        return new GithubAlertBlock(directive.alertType(), directive.children(), directive.remainingText());
    }

    public static @Nullable BlockquoteDirective parseBlockquoteDirective(MdAstBlockquote blockquote) {
        FirstParagraphText firstParagraph = findFirstParagraphText(blockquote);
        if (firstParagraph == null) {
            return null;
        }

        String firstText = firstParagraph.text();
        if (firstText == null || firstText.trim()
            .isEmpty()) {
            return null;
        }

        GithubAlertType alertType = GithubAlertType.fromDirective(firstText);
        if (alertType != null) {
            int directiveEnd = firstText.indexOf(']');
            return new BlockquoteDirective(
                alertType,
                alertType.accentColor(),
                alertType.displayText(),
                new QuoteIconSpec(QuoteIconKind.TEXT, alertType.symbol()),
                trimLeadingDirectiveText(firstText, directiveEnd >= 0 ? directiveEnd + 1 : 0),
                firstParagraph.paragraph(),
                blockquote.children());
        }

        String trimmed = firstText.trim();
        int directiveEnd = trimmed.indexOf('}');
        if (!trimmed.startsWith("{:") || directiveEnd < 0) {
            return null;
        }

        String expression = trimmed.substring(2, directiveEnd)
            .trim();
        String title = null;
        ColorValue color = null;
        QuoteIconSpec icon = null;
        for (String token : splitTokens(expression)) {
            int equalsIndex = token.indexOf('=');
            if (equalsIndex <= 0 || equalsIndex >= token.length() - 1) {
                continue;
            }
            String key = token.substring(0, equalsIndex)
                .trim();
            String value = stripOptionalQuotes(
                token.substring(equalsIndex + 1)
                    .trim());
            if (value.isEmpty()) {
                continue;
            }
            if ("title".equalsIgnoreCase(key)) {
                title = value;
            } else if ("color".equalsIgnoreCase(key)) {
                color = parseColor(value);
            } else if ("icon".equalsIgnoreCase(key)) {
                icon = new QuoteIconSpec(QuoteIconKind.TEXT, value);
            } else if ("iconPng".equalsIgnoreCase(key) || "icon_png".equalsIgnoreCase(key)) {
                icon = new QuoteIconSpec(QuoteIconKind.PNG, value);
            } else if ("iconItem".equalsIgnoreCase(key) || "icon_item".equalsIgnoreCase(key)) {
                icon = new QuoteIconSpec(QuoteIconKind.ITEM, value);
            }
        }

        if (title == null && color == null && icon == null) {
            return null;
        }

        return new BlockquoteDirective(
            null,
            color != null ? color : new ConstantColor(0xFF7C8795),
            title,
            icon,
            trimLeadingDirectiveText(trimmed, directiveEnd + 1),
            firstParagraph.paragraph(),
            blockquote.children());
    }

    private static @Nullable FirstParagraphText findFirstParagraphText(MdAstBlockquote blockquote) {
        for (MdAstAnyContent child : blockquote.children()) {
            if (child instanceof MdAstParagraph paragraph) {
                String text = getLeadingParagraphText(paragraph);
                if (text != null && !text.trim()
                    .isEmpty()) {
                    return new FirstParagraphText(paragraph, text);
                }
            } else if (child instanceof MdAstText text && !text.value.trim()
                .isEmpty()) {
                    return new FirstParagraphText(null, text.value);
                }
        }
        return null;
    }

    private static @Nullable String getLeadingParagraphText(MdAstParagraph paragraph) {
        for (MdAstAnyContent child : paragraph.children()) {
            if (child instanceof MdAstText text && !text.value.trim()
                .isEmpty()) {
                return text.value;
            }
            if (child instanceof MdAstInlineCode code && !code.value.trim()
                .isEmpty()) {
                return code.value;
            }
        }
        return null;
    }

    private static String trimLeadingDirectiveText(String text, int startIndex) {
        if (text == null || text.isEmpty() || startIndex < 0 || startIndex >= text.length()) {
            return "";
        }
        return text.substring(startIndex)
            .trim();
    }

    private static List<String> splitTokens(String expression) {
        if (expression == null || expression.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        char quote = 0;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if ((ch == '"' || ch == '\'') && (!inQuotes || ch == quote)) {
                if (inQuotes && ch == quote) {
                    inQuotes = false;
                    quote = 0;
                } else if (!inQuotes) {
                    inQuotes = true;
                    quote = ch;
                }
                current.append(ch);
                continue;
            }
            if (Character.isWhitespace(ch) && !inQuotes) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            current.append(ch);
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static String stripOptionalQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static @Nullable ColorValue parseColor(String value) {
        String normalized = value != null ? value.trim() : "";
        if (normalized.isEmpty() || !normalized.startsWith("#")) {
            return null;
        }
        try {
            if (normalized.length() == 7) {
                int rgb = Integer.parseInt(normalized.substring(1), 16);
                return new ConstantColor(0xFF000000 | rgb);
            }
            if (normalized.length() == 9) {
                long argb = Long.parseLong(normalized.substring(1), 16);
                return new ConstantColor((int) argb);
            }
        } catch (NumberFormatException ignored) {}
        return null;
    }

    @Desugar
    public record BlockquoteDirective(@Nullable GithubAlertType alertType, ColorValue accentColor,
        @Nullable String title, @Nullable QuoteIconSpec icon, String remainingText,
        @Nullable MdAstParagraph firstParagraph, List<? extends MdAstAnyContent> children) {}

    @Desugar
    public record QuoteIconSpec(QuoteIconKind kind, String value) {}

    public enum QuoteIconKind {
        TEXT,
        PNG,
        ITEM
    }

    @Desugar
    private record FirstParagraphText(@Nullable MdAstParagraph paragraph, String text) {}

    @Desugar
    public record GithubAlertBlock(GithubAlertType type, List<? extends MdAstAnyContent> children,
        String remainingText) {}
}
