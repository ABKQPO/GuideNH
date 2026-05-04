package com.hfstudio.guidenh.guide.document.block.functiongraph;

/**
 * Recursive-descent parser for function expressions used by the function graph block. The grammar is
 * intentionally permissive: implicit multiplication ({@code 2x}, {@code 3sin(x)}), Unicode square /
 * cube root prefixes, vertical-bar absolute value ({@code |expr|}), postfix factorial and Greek-style
 * constants are all accepted. On failure {@link #parse(String, int)} returns a constant NaN node so
 * downstream sampling simply produces a blank curve instead of throwing.
 *
 * <p>
 * Operator precedence (low to high): {@code +-} -> {@code * / %} -> unary minus / abs ->
 * {@code ^} (right associative) -> postfix {@code !} -> primary.
 */
public final class FunctionExprParser {

    private final String src;
    private final int defaultVar;
    private int pos;

    private FunctionExprParser(String src, int defaultVar) {
        this.src = src;
        this.defaultVar = defaultVar;
        this.pos = 0;
    }

    /**
     * Parse {@code source} producing an AST. {@code defaultVar} is the variable index assigned to a
     * bare identifier {@code x} or {@code y} when the expression does not use the explicit name.
     * Pass {@code 0} for normal {@code y = f(x)} plots and {@code 1} for inverse {@code x = f(y)}.
     */
    public static FunctionExpr parse(String source, int defaultVar) {
        if (source == null) {
            return new FunctionExpr.Constant(Double.NaN);
        }
        String trimmed = source.trim();
        if (trimmed.isEmpty()) {
            return new FunctionExpr.Constant(Double.NaN);
        }
        // Strip an optional 'y =' / 'x =' prefix so authors can write 'y = sin(x)' verbatim.
        trimmed = stripAssignmentPrefix(trimmed);
        FunctionExprParser parser = new FunctionExprParser(trimmed, defaultVar);
        try {
            FunctionExpr root = parser.parseAdditive();
            parser.skipWhitespace();
            if (parser.pos != parser.src.length()) {
                return new FunctionExpr.Constant(Double.NaN);
            }
            return root;
        } catch (RuntimeException ex) {
            return new FunctionExpr.Constant(Double.NaN);
        }
    }

    private static String stripAssignmentPrefix(String s) {
        int eq = s.indexOf('=');
        if (eq <= 0) {
            return s;
        }
        String head = s.substring(0, eq)
            .trim();
        if (head.equals("y") || head.equals("x") || head.equalsIgnoreCase("f(x)") || head.equalsIgnoreCase("f(y)")) {
            return s.substring(eq + 1)
                .trim();
        }
        return s;
    }

    private FunctionExpr parseAdditive() {
        FunctionExpr left = parseMultiplicative();
        while (true) {
            skipWhitespace();
            if (pos >= src.length()) {
                return left;
            }
            char c = src.charAt(pos);
            if (c == '+') {
                pos++;
                FunctionExpr right = parseMultiplicative();
                left = new FunctionExpr.Binary(FunctionExpr.Binary.ADD, left, right);
            } else if (c == '-' || c == '\u2212') {
                pos++;
                FunctionExpr right = parseMultiplicative();
                left = new FunctionExpr.Binary(FunctionExpr.Binary.SUB, left, right);
            } else {
                return left;
            }
        }
    }

    private FunctionExpr parseMultiplicative() {
        FunctionExpr left = parseUnary();
        while (true) {
            skipWhitespace();
            if (pos >= src.length()) {
                return left;
            }
            char c = src.charAt(pos);
            if (c == '*' || c == '\u00D7' || c == '\u22C5') {
                pos++;
                FunctionExpr right = parseUnary();
                left = new FunctionExpr.Binary(FunctionExpr.Binary.MUL, left, right);
            } else if (c == '/' || c == '\u00F7') {
                pos++;
                FunctionExpr right = parseUnary();
                left = new FunctionExpr.Binary(FunctionExpr.Binary.DIV, left, right);
            } else if (c == '%') {
                pos++;
                FunctionExpr right = parseUnary();
                left = new FunctionExpr.Binary(FunctionExpr.Binary.MOD, left, right);
            } else if (isImplicitMultiplyStart(c)) {
                FunctionExpr right = parseUnary();
                left = new FunctionExpr.Binary(FunctionExpr.Binary.MUL, left, right);
            } else {
                return left;
            }
        }
    }

    private boolean isImplicitMultiplyStart(char c) {
        // An identifier or '(' immediately following a primary triggers implicit multiplication.
        // '|' is intentionally excluded: after parsing a primary such as "x", the closing '|' of
        // an enclosing "|x|" expression would otherwise be mistaken for the opening of a new
        // absolute-value operand, breaking expressions like "|x| - 1".
        if (c == '(') {
            return true;
        }
        return Character.isLetter(c) || c == '\u221A' || c == '\u221B';
    }

    private FunctionExpr parseUnary() {
        skipWhitespace();
        if (pos >= src.length()) {
            throw new RuntimeException("unexpected end of input");
        }
        char c = src.charAt(pos);
        if (c == '+') {
            pos++;
            return parseUnary();
        }
        if (c == '-' || c == '\u2212') {
            pos++;
            return new FunctionExpr.Neg(parseUnary());
        }
        if (c == '\u221A') {
            pos++;
            return new FunctionExpr.Call("sqrt", new FunctionExpr[] { parseUnary() });
        }
        if (c == '\u221B') {
            pos++;
            return new FunctionExpr.Call("cbrt", new FunctionExpr[] { parseUnary() });
        }
        return parsePower();
    }

    private FunctionExpr parsePower() {
        FunctionExpr base = parsePostfix();
        skipWhitespace();
        if (pos < src.length()) {
            char c = src.charAt(pos);
            if (c == '^' || c == '\u2227') {
                pos++;
                // Right associative.
                FunctionExpr exponent = parseUnary();
                return new FunctionExpr.Binary(FunctionExpr.Binary.POW, base, exponent);
            }
            if (c == '\u00B2') {
                pos++;
                return new FunctionExpr.Binary(FunctionExpr.Binary.POW, base, new FunctionExpr.Constant(2d));
            }
            if (c == '\u00B3') {
                pos++;
                return new FunctionExpr.Binary(FunctionExpr.Binary.POW, base, new FunctionExpr.Constant(3d));
            }
        }
        return base;
    }

    private FunctionExpr parsePostfix() {
        FunctionExpr inner = parsePrimary();
        while (true) {
            skipWhitespace();
            if (pos >= src.length()) {
                return inner;
            }
            char c = src.charAt(pos);
            if (c == '!') {
                pos++;
                inner = new FunctionExpr.Factorial(inner);
            } else {
                return inner;
            }
        }
    }

    private FunctionExpr parsePrimary() {
        skipWhitespace();
        if (pos >= src.length()) {
            throw new RuntimeException("unexpected end of input");
        }
        char c = src.charAt(pos);
        if (c == '(') {
            pos++;
            FunctionExpr inner = parseAdditive();
            skipWhitespace();
            if (pos >= src.length() || src.charAt(pos) != ')') {
                throw new RuntimeException("missing ')'");
            }
            pos++;
            return inner;
        }
        if (c == '|') {
            pos++;
            FunctionExpr inner = parseAdditive();
            skipWhitespace();
            if (pos >= src.length() || src.charAt(pos) != '|') {
                throw new RuntimeException("missing '|'");
            }
            pos++;
            return new FunctionExpr.Abs(inner);
        }
        if (Character.isDigit(c) || c == '.') {
            return parseNumber();
        }
        if (Character.isLetter(c) || c == '_' || c == '\u03C0' || c == '\u03C4') {
            return parseIdentifierOrCall();
        }
        throw new RuntimeException("unexpected character '" + c + "'");
    }

    private FunctionExpr parseNumber() {
        int start = pos;
        boolean dot = false;
        boolean exp = false;
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isDigit(c)) {
                pos++;
            } else if (c == '.' && !dot && !exp) {
                dot = true;
                pos++;
            } else if ((c == 'e' || c == 'E') && !exp) {
                exp = true;
                pos++;
                if (pos < src.length() && (src.charAt(pos) == '+' || src.charAt(pos) == '-')) {
                    pos++;
                }
            } else {
                break;
            }
        }
        String text = src.substring(start, pos);
        try {
            return new FunctionExpr.Constant(Double.parseDouble(text));
        } catch (NumberFormatException ex) {
            return new FunctionExpr.Constant(Double.NaN);
        }
    }

    private FunctionExpr parseIdentifierOrCall() {
        int start = pos;
        // Handle bare Unicode pi / tau as constants.
        char first = src.charAt(pos);
        if (first == '\u03C0') {
            pos++;
            return new FunctionExpr.Constant(Math.PI);
        }
        if (first == '\u03C4') {
            pos++;
            return new FunctionExpr.Constant(2d * Math.PI);
        }
        while (pos < src.length()) {
            char c = src.charAt(pos);
            if (Character.isLetterOrDigit(c) || c == '_') {
                pos++;
            } else {
                break;
            }
        }
        String name = src.substring(start, pos);
        skipWhitespace();
        if (pos < src.length() && src.charAt(pos) == '(') {
            pos++;
            FunctionExpr first1 = parseAdditive();
            skipWhitespace();
            FunctionExpr second = null;
            if (pos < src.length() && src.charAt(pos) == ',') {
                pos++;
                second = parseAdditive();
                skipWhitespace();
            }
            if (pos >= src.length() || src.charAt(pos) != ')') {
                throw new RuntimeException("missing ')'");
            }
            pos++;
            FunctionExpr[] args = second == null ? new FunctionExpr[] { first1 }
                : new FunctionExpr[] { first1, second };
            if (!FunctionLibrary.isKnown(name)) {
                throw new RuntimeException("unknown function: " + name);
            }
            return new FunctionExpr.Call(name, args);
        }
        // Bare identifier: variable or constant.
        if (name.equals("x")) {
            return new FunctionExpr.Variable(0);
        }
        if (name.equals("y")) {
            return new FunctionExpr.Variable(1);
        }
        double constant = FunctionLibrary.constant(name);
        if (!Double.isNaN(constant)) {
            return new FunctionExpr.Constant(constant);
        }
        // Unknown single-letter identifier: fall back to the default variable so users can write
        // expressions like {@code t} when that is the configured input.
        if (name.length() == 1) {
            return new FunctionExpr.Variable(defaultVar);
        }
        throw new RuntimeException("unknown identifier: " + name);
    }

    private void skipWhitespace() {
        while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
            pos++;
        }
    }
}
