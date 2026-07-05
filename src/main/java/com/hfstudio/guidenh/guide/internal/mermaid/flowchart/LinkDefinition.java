package com.hfstudio.guidenh.guide.internal.mermaid.flowchart;

import org.jetbrains.annotations.Nullable;

import com.hfstudio.guidenh.guide.internal.mermaid.MermaidArrowHead;
import com.hfstudio.guidenh.guide.internal.mermaid.MermaidEdgeStyle;

public enum LinkDefinition {

    // ── Solid ──────────────────────────────────────────────
    SOLID_ARROW("-->", MermaidEdgeStyle.SOLID, true, false, MermaidArrowHead.TRIANGLE, MermaidArrowHead.NONE, '-', 2),
    SOLID_LINK("---", MermaidEdgeStyle.SOLID, false, false, MermaidArrowHead.NONE, MermaidArrowHead.NONE, '-', 2),
    SOLID_REV("<---", MermaidEdgeStyle.SOLID, false, true, MermaidArrowHead.NONE, MermaidArrowHead.TRIANGLE, '-', 2),
    SOLID_BOTH("<-->", MermaidEdgeStyle.SOLID, true, true, MermaidArrowHead.TRIANGLE, MermaidArrowHead.TRIANGLE, '-',
        2),
    SOLID_CIRCLE_FWD("--o", MermaidEdgeStyle.SOLID, true, false, MermaidArrowHead.CIRCLE, MermaidArrowHead.NONE, '-',
        2),
    SOLID_CIRCLE_REV("o--", MermaidEdgeStyle.SOLID, false, true, MermaidArrowHead.NONE, MermaidArrowHead.CIRCLE, '-',
        2),
    SOLID_CIRCLE_BOTH("o--o", MermaidEdgeStyle.SOLID, true, true, MermaidArrowHead.CIRCLE, MermaidArrowHead.CIRCLE, '-',
        2),
    SOLID_CROSS_FWD("--x", MermaidEdgeStyle.SOLID, true, false, MermaidArrowHead.CROSS, MermaidArrowHead.NONE, '-', 2),
    SOLID_CROSS_REV("x--", MermaidEdgeStyle.SOLID, false, true, MermaidArrowHead.NONE, MermaidArrowHead.CROSS, '-', 2),
    SOLID_CROSS_BOTH("x--x", MermaidEdgeStyle.SOLID, true, true, MermaidArrowHead.CROSS, MermaidArrowHead.CROSS, '-',
        2),

    // ── Thick ──────────────────────────────────────────────
    THICK_ARROW("==>", MermaidEdgeStyle.THICK, true, false, MermaidArrowHead.TRIANGLE, MermaidArrowHead.NONE, '=', 2),
    THICK_LINK("===", MermaidEdgeStyle.THICK, false, false, MermaidArrowHead.NONE, MermaidArrowHead.NONE, '=', 2),
    THICK_REV("<===", MermaidEdgeStyle.THICK, false, true, MermaidArrowHead.NONE, MermaidArrowHead.TRIANGLE, '=', 2),
    THICK_BOTH("<=>", MermaidEdgeStyle.THICK, true, true, MermaidArrowHead.TRIANGLE, MermaidArrowHead.TRIANGLE, '=', 2),
    THICK_CIRCLE_FWD("==o", MermaidEdgeStyle.THICK, true, false, MermaidArrowHead.CIRCLE, MermaidArrowHead.NONE, '=',
        2),
    THICK_CIRCLE_REV("o==", MermaidEdgeStyle.THICK, false, true, MermaidArrowHead.NONE, MermaidArrowHead.CIRCLE, '=',
        2),
    THICK_CIRCLE_BOTH("o==o", MermaidEdgeStyle.THICK, true, true, MermaidArrowHead.CIRCLE, MermaidArrowHead.CIRCLE, '=',
        2),
    THICK_CROSS_FWD("==x", MermaidEdgeStyle.THICK, true, false, MermaidArrowHead.CROSS, MermaidArrowHead.NONE, '=', 2),
    THICK_CROSS_REV("x==", MermaidEdgeStyle.THICK, false, true, MermaidArrowHead.NONE, MermaidArrowHead.CROSS, '=', 2),
    THICK_CROSS_BOTH("x==x", MermaidEdgeStyle.THICK, true, true, MermaidArrowHead.CROSS, MermaidArrowHead.CROSS, '=',
        2),

    // ── Dashed (dot) ───────────────────────────────────────
    DASHED_ARROW("-.->", MermaidEdgeStyle.DASHED, true, false, MermaidArrowHead.TRIANGLE, MermaidArrowHead.NONE, '.',
        1),
    DASHED_LINK("-.-", MermaidEdgeStyle.DASHED, false, false, MermaidArrowHead.NONE, MermaidArrowHead.NONE, '.', 1),
    DASHED_REV("<-.--", MermaidEdgeStyle.DASHED, false, true, MermaidArrowHead.NONE, MermaidArrowHead.TRIANGLE, '.', 1),
    DASHED_BOTH("<-.->", MermaidEdgeStyle.DASHED, true, true, MermaidArrowHead.TRIANGLE, MermaidArrowHead.TRIANGLE, '.',
        1),
    DASHED_CIRCLE_FWD("-.o", MermaidEdgeStyle.DASHED, true, false, MermaidArrowHead.CIRCLE, MermaidArrowHead.NONE, '.',
        1),
    DASHED_CIRCLE_REV("o-.", MermaidEdgeStyle.DASHED, false, true, MermaidArrowHead.NONE, MermaidArrowHead.CIRCLE, '.',
        1),

    // ── Dotted (tilde) ─────────────────────────────────────
    DOTTED_ARROW("~~>", MermaidEdgeStyle.DOTTED, true, false, MermaidArrowHead.TRIANGLE, MermaidArrowHead.NONE, '~', 2),
    DOTTED_LINK("~~~", MermaidEdgeStyle.DOTTED, false, false, MermaidArrowHead.NONE, MermaidArrowHead.NONE, '~', 2),
    DOTTED_REV("<~~~", MermaidEdgeStyle.DOTTED, false, true, MermaidArrowHead.NONE, MermaidArrowHead.TRIANGLE, '~', 2),
    DOTTED_BOTH("<~~>", MermaidEdgeStyle.DOTTED, true, true, MermaidArrowHead.TRIANGLE, MermaidArrowHead.TRIANGLE, '~',
        2),

    // ── Invisible ──────────────────────────────────────────
    INVISIBLE_LINK("~~~", MermaidEdgeStyle.INVISIBLE, false, false, MermaidArrowHead.NONE, MermaidArrowHead.NONE, '~',
        3);

    private final String syntax;
    private final MermaidEdgeStyle style;
    private final boolean arrowFwd;
    private final boolean arrowRev;
    private final MermaidArrowHead forwardHead;
    private final MermaidArrowHead reverseHead;
    private final char repeatChar;
    private final int minRepeat;

    LinkDefinition(String syntax, MermaidEdgeStyle style, boolean arrowFwd, boolean arrowRev,
        MermaidArrowHead forwardHead, MermaidArrowHead reverseHead, char repeatChar, int minRepeat) {
        this.syntax = syntax;
        this.style = style;
        this.arrowFwd = arrowFwd;
        this.arrowRev = arrowRev;
        this.forwardHead = forwardHead;
        this.reverseHead = reverseHead;
        this.repeatChar = repeatChar;
        this.minRepeat = minRepeat;
    }

    public String syntax() {
        return syntax;
    }

    public MermaidEdgeStyle style() {
        return style;
    }

    public boolean arrowFwd() {
        return arrowFwd;
    }

    public boolean arrowRev() {
        return arrowRev;
    }

    public MermaidArrowHead forwardHead() {
        return forwardHead;
    }

    public MermaidArrowHead reverseHead() {
        return reverseHead;
    }

    public char repeatChar() {
        return repeatChar;
    }

    public int minRepeat() {
        return minRepeat;
    }

    /** True if this link has variable length (repeatChar is significant). */
    public boolean isVariableLength() {
        return repeatChar != '\0' && minRepeat > 0 && repeatChar != '.';
    }

    /**
     * Try to find the best matching {@link LinkDefinition} at or after a given
     * start index in {@code text}. Returns the match result, or {@code null}
     * if no link pattern is found.
     */
    @Nullable
    public static MatchResult matchAt(String text, int start) {
        if (text == null || start < 0 || start >= text.length()) return null;

        LinkDefinition best = null;
        int bestPos = Integer.MAX_VALUE;
        int bestLen = 0;

        for (LinkDefinition def : values()) {
            int idx = text.indexOf(def.syntax, start);
            if (idx < 0) continue;
            if (idx < bestPos || (idx == bestPos && def.syntax.length() > bestLen)) {
                bestPos = idx;
                bestLen = def.syntax.length();
                best = def;
            }
        }

        for (LinkDefinition def : values()) {
            if (!def.arrowFwd() || !def.arrowRev()) continue;
            if (!def.syntax()
                .startsWith("<")
                || !def.syntax()
                    .endsWith(">"))
                continue;
            char rc = def.repeatChar();
            if (rc == '\0') continue;
            for (int i = start; i < text.length(); i++) {
                if (text.charAt(i) != '<') continue;
                int j = i + 1;
                while (j < text.length() && text.charAt(j) == rc) j++;
                if (j - i - 1 >= def.minRepeat() && j < text.length() && text.charAt(j) == '>') {
                    int matchLen = j - i + 1;
                    if (i < bestPos || (i == bestPos && matchLen >= bestLen)) {
                        bestPos = i;
                        bestLen = matchLen;
                        best = def;
                    }
                }
            }
        }
        if (best == null) return null;

        int actualLen = bestLen;
        if (best.isVariableLength()) {
            int count = 0;
            for (int i = bestPos; i < text.length(); i++) {
                if (text.charAt(i) == best.repeatChar) count++;
                else break;
            }
            actualLen = Math.max(actualLen, bestPos + count - bestPos);
        }

        // handle dotted/dashed length (".-")
        if (best.repeatChar == '.') {
            int dotCount = 0;
            for (int i = bestPos; i < text.length(); i++) {
                if (text.charAt(i) == '.') dotCount++;
                else break;
            }
            actualLen = best.syntax.length() + Math.max(0, dotCount - 1);
        }

        return new MatchResult(best, bestPos, actualLen);
    }

    /**
     * Scan the full {@code text} for the first link pattern, returning its
     * match result, or {@code null} if none found.
     */
    @Nullable
    public static MatchResult findFirst(String text) {
        return matchAt(text, 0);
    }

    public record MatchResult(LinkDefinition definition, int position, int length) {}
}
