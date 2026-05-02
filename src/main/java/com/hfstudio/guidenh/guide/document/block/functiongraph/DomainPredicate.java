package com.hfstudio.guidenh.guide.document.block.functiongraph;

/**
 * Predicate restricting where a {@link FunctionPlot} is sampled. Predicates are combined by chaining
 * {@code and(...)}; the renderer treats {@link Double#NaN}-returning samples and false predicates as
 * polyline breaks.
 */
public interface DomainPredicate {

    /** Constant-true singleton. */
    DomainPredicate ALWAYS = (x, y) -> true;

    boolean accepts(double x, double y);

    /** Returns a predicate satisfied when both {@code this} and {@code other} accept. */
    default DomainPredicate and(DomainPredicate other) {
        if (other == null || other == ALWAYS) {
            return this;
        }
        DomainPredicate self = this;
        return (x, y) -> self.accepts(x, y) && other.accepts(x, y);
    }

    /**
     * Build a predicate from a {@code min..max} domain string applied to the x variable. {@code null}
     * or empty bounds become {@link Double#NEGATIVE_INFINITY} / {@link Double#POSITIVE_INFINITY}.
     */
    static DomainPredicate xRange(double min, double max) {
        return (x, y) -> x >= min && x <= max;
    }

    /** Same as {@link #xRange(double, double)} but applied to the {@code y} variable. */
    static DomainPredicate yRange(double min, double max) {
        return (x, y) -> y >= min && y <= max;
    }

    /**
     * Parse a comma-separated list of clauses such as {@code "x>=0, x<5"}. Unsupported clauses are
     * silently ignored.
     */
    static DomainPredicate parse(String spec) {
        if (spec == null) {
            return ALWAYS;
        }
        String trimmed = spec.trim();
        if (trimmed.isEmpty()) {
            return ALWAYS;
        }
        DomainPredicate result = ALWAYS;
        String[] clauses = trimmed.split(",");
        for (String raw : clauses) {
            DomainPredicate clause = parseClause(raw.trim());
            if (clause != null) {
                result = result.and(clause);
            }
        }
        return result;
    }

    /** Parse a single clause; returns {@code null} on failure. */
    static DomainPredicate parseClause(String clause) {
        if (clause.isEmpty()) {
            return null;
        }
        // Range clause '<min>..<max>' (relative to x).
        int dotDot = clause.indexOf("..");
        if (dotDot >= 0) {
            String head = clause.substring(0, dotDot)
                .trim();
            String tail = clause.substring(dotDot + 2)
                .trim();
            double min = parseNumberOrConstant(head, Double.NEGATIVE_INFINITY);
            double max = parseNumberOrConstant(tail, Double.POSITIVE_INFINITY);
            return xRange(min, max);
        }
        // Comparison clauses involving x or y. Supported operators: < <= > >= = ==.
        String[] ops = { "<=", ">=", "==", "<", ">", "=" };
        for (String op : ops) {
            int idx = clause.indexOf(op);
            if (idx > 0) {
                String left = clause.substring(0, idx)
                    .trim();
                String right = clause.substring(idx + op.length())
                    .trim();
                if (left.equals("x") || left.equals("y")) {
                    int which = left.equals("x") ? 0 : 1;
                    double v = parseNumberOrConstant(right, Double.NaN);
                    if (Double.isNaN(v)) {
                        return null;
                    }
                    return makeComparison(which, op, v, false);
                }
                if (right.equals("x") || right.equals("y")) {
                    int which = right.equals("x") ? 0 : 1;
                    double v = parseNumberOrConstant(left, Double.NaN);
                    if (Double.isNaN(v)) {
                        return null;
                    }
                    return makeComparison(which, op, v, true);
                }
            }
        }
        return null;
    }

    /** Build a comparison predicate. {@code reversed} indicates literal-on-left layout. */
    static DomainPredicate makeComparison(int which, String op, double v, boolean reversed) {
        return (x, y) -> {
            double lhs = which == 0 ? x : y;
            double a = reversed ? v : lhs;
            double b = reversed ? lhs : v;
            return switch (op) {
                case "<" -> a < b;
                case "<=" -> a <= b;
                case ">" -> a > b;
                case ">=" -> a >= b;
                case "=", "==" -> Math.abs(a - b) < 1e-9;
                default -> true;
            };
        };
    }

    /** Parse a numeric literal allowing the constants {@code pi}, {@code e}, {@code tau}. */
    static double parseNumberOrConstant(String text, double fallback) {
        if (text == null || text.isEmpty()) {
            return fallback;
        }
        double constant = FunctionLibrary.constant(text);
        if (!Double.isNaN(constant)) {
            return constant;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
