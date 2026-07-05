package com.hfstudio.guidenh.guide.internal.mermaid;

import org.jetbrains.annotations.Nullable;

/**
 * Maps every node shape from the jison {@code vertex:} grammar rules to its
 * opening / closing delimiter pair and the corresponding {@link MermaidNodeShape}.
 *
 * <p>
 * Adding a new shape is as simple as adding a new enum constant.
 */
public enum NodeShapeDefinition {

    ELLIPSE("(-", "-)", MermaidNodeShape.ELLIPSE),

    // [ variants — all before SQUARE (which is a single '[')
    STADIUM("([", "])", MermaidNodeShape.STADIUM),
    SUBROUTINE("[[", "]]", MermaidNodeShape.SUBPROCESS),
    CYLINDER("[(", ")]", MermaidNodeShape.CYLINDER),
    TRAPEZOID("[/", "\\]", MermaidNodeShape.TRAPEZOID),
    LEAN_RIGHT("[/", "/]", MermaidNodeShape.ASYMMETRIC),
    INV_TRAPEZOID("[\\", "/]", MermaidNodeShape.TRAPEZOID),
    LEAN_LEFT("[\\", "\\]", MermaidNodeShape.ASYMMETRIC),
    SQUARE('[', ']', MermaidNodeShape.SQUARE),

    // ( variants — all before ROUND (which is a single '(')
    DOUBLE_CIRCLE("(((", ")))", MermaidNodeShape.DOUBLE_CIRCLE),
    CIRCLE("((", "))", MermaidNodeShape.CIRCLE),
    ROUND('(', ')', MermaidNodeShape.ROUNDED),

    // { variants
    HEXAGON("{{", "}}", MermaidNodeShape.HEXAGON),
    DIAMOND('{', '}', MermaidNodeShape.DIAMOND),

    ASYMMETRIC('>', ']', MermaidNodeShape.ASYMMETRIC),

    // ) variants — BANG before CLOUD (both start with ))
    BANG("))", "((", MermaidNodeShape.BANG),
    CLOUD(')', '(', MermaidNodeShape.CLOUD);

    private final String open;
    private final String close;
    private final MermaidNodeShape shape;

    NodeShapeDefinition(char open, char close, MermaidNodeShape shape) {
        this(String.valueOf(open), String.valueOf(close), shape);
    }

    NodeShapeDefinition(String open, String close, MermaidNodeShape shape) {
        this.open = open;
        this.close = close;
        this.shape = shape;
    }

    public String open() {
        return open;
    }

    public String close() {
        return close;
    }

    public MermaidNodeShape shape() {
        return shape;
    }

    /**
     * Try to match any known node-shape delimiter pair in {@code text}.
     * Returns a {@link MatchResult} if a shape is found, or {@code null}.
     */
    @Nullable
    public static MatchResult match(String text) {
        if (text == null || text.isEmpty()) return null;

        // double-circle must be tried before circle (3-paren vs 2-paren)
        // hexagon before diamond ({{ vs {)
        // subroutine before square ([[ vs [)
        // cylinder before round ([ ( vs ( )
        for (NodeShapeDefinition def : values()) {
            if (text.startsWith(def.open) && text.endsWith(def.close)) {
                String inner = text.substring(def.open.length(), text.length() - def.close.length())
                    .trim();
                // double-circle with ((())) — inner wrapped in extra parens
                if (def == DOUBLE_CIRCLE && inner.startsWith("(") && inner.endsWith(")")) {
                    return new MatchResult(
                        DOUBLE_CIRCLE,
                        inner.substring(1, inner.length() - 1)
                            .trim());
                }
                return new MatchResult(def, inner);
            }
        }
        // If a trailing ] is present, the text might end with "[/...\]" where
        // the close is literally backslash+bracket — the above already handles
        // this via the "\\]" close string.
        return null;
    }

    public record MatchResult(NodeShapeDefinition definition, String inner) {}
}
