package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.github.bsideup.jabel.Desugar;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxAttribute;
import com.hfstudio.guidenh.libs.mdast.mdx.model.MdxJsxElementFields;
import com.hfstudio.guidenh.libs.mdast.model.MdAstLiteral;
import com.hfstudio.guidenh.libs.mdast.model.MdAstNode;
import com.hfstudio.guidenh.libs.mdast.model.MdAstParent;

/**
 * Utility for detecting and splitting {@code $$formula$$} shorthand LaTeX expressions
 * inside Markdown text nodes.
 *
 * <p>
 * A {@code $$formula$$} shorthand always uses default rendering parameters (white colour,
 * scale 1.0, no tooltip). For full control over appearance use the {@code <Latex>} tag instead.
 *
 * <p>
 * Display-mode detection: if a paragraph's only text content is exactly {@code $$formula$$}
 * (after trimming whitespace), the formula is rendered as a centred display block. Otherwise
 * each {@code $$formula$$} fragment is rendered as an inline block inside the surrounding text.
 */
public class MarkdownLatexShorthand {

    private static final String PLACEHOLDER_PREFIX = "\uE000GUIDENH_LATEX_";
    private static final String PLACEHOLDER_SUFFIX = "_\uE001";

    /**
     * Matches {@code $$...$$} where the content contains no literal {@code $} characters.
     * DOTALL allows newlines inside the formula.
     */
    private static final Pattern DOLLAR_PATTERN = Pattern.compile("\\$\\$([^$]+?)\\$\\$", Pattern.DOTALL);

    private MarkdownLatexShorthand() {}

    public static MaskResult mask(String source) {
        if (source == null) {
            return new MaskResult("", Map.of());
        }
        if (!mayContain(source)) {
            return new MaskResult(source, Map.of());
        }
        Matcher matcher = DOLLAR_PATTERN.matcher(source);
        StringBuilder masked = new StringBuilder(source.length());
        Map<String, String> formulas = new HashMap<>();
        int index = 0;
        while (matcher.find()) {
            String placeholder = PLACEHOLDER_PREFIX + index + PLACEHOLDER_SUFFIX;
            formulas.put(placeholder, matcher.group(1));
            matcher.appendReplacement(masked, Matcher.quoteReplacement("$$" + placeholder + "$$"));
            index++;
        }
        matcher.appendTail(masked);
        return new MaskResult(masked.toString(), formulas);
    }

    public static void restore(MdAstNode root, MaskResult maskResult) {
        if (maskResult == null || maskResult.isEmpty() || root == null) {
            return;
        }
        restoreNode(root, maskResult);
    }

    /**
     * Quick pre-check: returns {@code false} if {@code text} cannot contain any {@code $$} pattern.
     */
    public static boolean mayContain(String text) {
        return text != null && text.contains("$$");
    }

    /**
     * If {@code text}, when trimmed, is exactly one {@code $$formula$$} expression,
     * returns the formula content; otherwise returns {@code null}.
     *
     * @param text the raw text value of an AST text node
     * @return the formula string, or {@code null} if the text is not a sole display formula
     */
    @Nullable
    public static String extractSoleDisplayFormula(String text) {
        if (!mayContain(text)) {
            return null;
        }
        String trimmed = text.trim();
        Matcher m = DOLLAR_PATTERN.matcher(trimmed);
        if (!m.matches()) {
            return null;
        }
        String formula = m.group(1)
            .trim();
        return formula.isEmpty() ? null : formula;
    }

    /**
     * Splits {@code text} into alternating plain-text and LaTeX-formula {@link Segment}s.
     * Plain-text segments may be empty strings only when the text starts or ends with a formula.
     *
     * @param text the raw text to split
     * @return ordered list of segments; never {@code null}
     */
    public static List<Segment> split(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        List<Segment> result = new ArrayList<>();
        Matcher m = DOLLAR_PATTERN.matcher(text);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                result.add(Segment.text(text.substring(last, m.start())));
            }
            result.add(Segment.formula(m.group(1)));
            last = m.end();
        }
        if (last < text.length()) {
            result.add(Segment.text(text.substring(last)));
        }
        return result;
    }

    private static void restoreNode(MdAstNode node, MaskResult maskResult) {
        if (node instanceof MdAstLiteral literal) {
            literal.value = restoreText(literal.value, maskResult);
        }
        if (node instanceof MdxJsxAttribute attribute) {
            restoreAttribute(attribute, maskResult);
        }
        if (node instanceof MdxJsxElementFields element) {
            for (Object attribute : element.attributes()) {
                if (attribute instanceof MdAstNode attributeNode) {
                    restoreNode(attributeNode, maskResult);
                }
            }
        }
        if (node instanceof MdAstParent<?>parent) {
            for (Object child : parent.children()) {
                if (child instanceof MdAstNode childNode) {
                    restoreNode(childNode, maskResult);
                }
            }
        } else if (node instanceof MdxJsxElementFields element) {
            for (Object child : element.children()) {
                if (child instanceof MdAstNode childNode) {
                    restoreNode(childNode, maskResult);
                }
            }
        }
    }

    private static String restoreText(String text, MaskResult maskResult) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String restored = text;
        for (Map.Entry<String, String> entry : maskResult.formulas()
            .entrySet()) {
            restored = restored.replace(entry.getKey(), entry.getValue());
        }
        return restored;
    }

    private static void restoreAttribute(MdxJsxAttribute attribute, MaskResult maskResult) {
        if (attribute.hasStringValue()) {
            attribute.setValue(restoreText(attribute.getStringValue(), maskResult));
        } else if (attribute.hasExpressionValue()) {
            attribute.setExpression(restoreText(attribute.getExpressionValue(), maskResult));
        }
    }

    @Desugar
    public record MaskResult(String source, Map<String, String> formulas) {

        public MaskResult {
            formulas = formulas == null ? Map.of() : Map.copyOf(new HashMap<>(formulas));
        }

        public boolean isEmpty() {
            return formulas.isEmpty();
        }
    }

    /** A text-or-formula segment produced by {@link #split}. */
    public static final class Segment {

        private final String value;
        private final boolean formula;

        private Segment(String value, boolean formula) {
            this.value = value;
            this.formula = formula;
        }

        /** Creates a plain-text segment. */
        public static Segment text(String value) {
            return new Segment(value, false);
        }

        /** Creates a LaTeX formula segment. */
        public static Segment formula(String value) {
            return new Segment(value, true);
        }

        /** Returns {@code true} if this segment holds a LaTeX formula. */
        public boolean isFormula() {
            return formula;
        }

        /** Returns the raw text or formula string. */
        public String getValue() {
            return value;
        }
    }
}
