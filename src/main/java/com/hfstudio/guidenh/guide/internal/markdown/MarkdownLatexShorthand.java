package com.hfstudio.guidenh.guide.internal.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

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
public final class MarkdownLatexShorthand {

    /**
     * Matches {@code $$...$$} where the content contains no literal {@code $} characters.
     * DOTALL allows newlines inside the formula.
     */
    private static final Pattern DOLLAR_PATTERN = Pattern.compile("\\$\\$([^$]+?)\\$\\$", Pattern.DOTALL);

    private MarkdownLatexShorthand() {}

    /**
     * Quick pre-check: returns {@code false} if {@code text} cannot contain any {@code $$} pattern.
     */
    public static boolean mayContain(String text) {
        return text.contains("$$");
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
