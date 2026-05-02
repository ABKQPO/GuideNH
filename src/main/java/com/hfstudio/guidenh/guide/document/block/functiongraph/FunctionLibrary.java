package com.hfstudio.guidenh.guide.document.block.functiongraph;

/**
 * Numeric helpers for the function graph: standard one- and two-argument numeric functions, plus a
 * Lanczos approximation of the gamma function that powers the postfix factorial operator.
 */
public final class FunctionLibrary {

    private FunctionLibrary() {}

    /** Returns true when {@code name} is recognised as a built-in function (any arity). */
    public static boolean isKnown(String name) {
        return resolveArity(name) != 0;
    }

    /** Returns 1 for unary functions, 2 for binary, otherwise 0. */
    public static int resolveArity(String name) {
        switch (name) {
            case "sin":
            case "cos":
            case "tan":
            case "asin":
            case "acos":
            case "atan":
            case "sinh":
            case "cosh":
            case "tanh":
            case "ln":
            case "log":
            case "log2":
            case "log10":
            case "exp":
            case "sqrt":
            case "cbrt":
            case "abs":
            case "sign":
            case "floor":
            case "ceil":
            case "round":
            case "deg":
            case "rad":
            case "gamma":
                return 1;
            case "atan2":
            case "min":
            case "max":
            case "pow":
            case "hypot":
            case "mod":
                return 2;
            default:
                return 0;
        }
    }

    /** Recognised mathematical constants. Returns {@link Double#NaN} when the name is unknown. */
    public static double constant(String name) {
        switch (name) {
            case "pi":
            case "PI":
            case "Pi":
                return Math.PI;
            case "e":
            case "E":
                return Math.E;
            case "tau":
            case "TAU":
            case "Tau":
                return Math.PI * 2d;
            case "phi":
            case "PHI":
                return (1d + Math.sqrt(5d)) / 2d;
            default:
                return Double.NaN;
        }
    }

    /** Evaluate a unary built-in. */
    public static double call1(String name, double a) {
        switch (name) {
            case "sin":
                return Math.sin(a);
            case "cos":
                return Math.cos(a);
            case "tan":
                return Math.tan(a);
            case "asin":
                return Math.asin(a);
            case "acos":
                return Math.acos(a);
            case "atan":
                return Math.atan(a);
            case "sinh":
                return Math.sinh(a);
            case "cosh":
                return Math.cosh(a);
            case "tanh":
                return Math.tanh(a);
            case "ln":
                return Math.log(a);
            case "log":
                return Math.log10(a);
            case "log2":
                return Math.log(a) / Math.log(2d);
            case "log10":
                return Math.log10(a);
            case "exp":
                return Math.exp(a);
            case "sqrt":
                return a < 0d ? Double.NaN : Math.sqrt(a);
            case "cbrt":
                return Math.cbrt(a);
            case "abs":
                return Math.abs(a);
            case "sign":
                return Math.signum(a);
            case "floor":
                return Math.floor(a);
            case "ceil":
                return Math.ceil(a);
            case "round":
                return Math.rint(a);
            case "deg":
                return Math.toDegrees(a);
            case "rad":
                return Math.toRadians(a);
            case "gamma":
                return gamma(a);
            default:
                return Double.NaN;
        }
    }

    /** Evaluate a binary built-in. */
    public static double call2(String name, double a, double b) {
        switch (name) {
            case "atan2":
                return Math.atan2(a, b);
            case "min":
                return Math.min(a, b);
            case "max":
                return Math.max(a, b);
            case "pow":
                return Math.pow(a, b);
            case "hypot":
                return Math.hypot(a, b);
            case "mod":
                return b == 0d ? Double.NaN : a % b;
            default:
                return Double.NaN;
        }
    }

    /**
     * Postfix factorial. For non-negative integers returns the exact factorial; otherwise falls back
     * to {@link #gamma(double) gamma}{@code (n + 1)}. Negative integers are undefined and return NaN.
     */
    public static double factorial(double n) {
        if (Double.isNaN(n)) {
            return Double.NaN;
        }
        double rounded = Math.rint(n);
        if (Math.abs(n - rounded) < 1e-9 && rounded >= 0d && rounded < 171d) {
            double acc = 1d;
            for (int i = 2; i <= (int) rounded; i++) {
                acc *= i;
            }
            return acc;
        }
        if (Math.abs(n - rounded) < 1e-9 && rounded < 0d) {
            return Double.NaN;
        }
        return gamma(n + 1d);
    }

    /**
     * Lanczos approximation of the gamma function. Accurate to roughly 15 significant digits for
     * positive arguments; uses the reflection formula for negative arguments.
     */
    public static double gamma(double z) {
        if (Double.isNaN(z)) {
            return Double.NaN;
        }
        if (z < 0.5d) {
            // Reflection: gamma(z) * gamma(1 - z) = pi / sin(pi * z).
            double sinPiZ = Math.sin(Math.PI * z);
            if (sinPiZ == 0d) {
                return Double.NaN;
            }
            return Math.PI / (sinPiZ * gamma(1d - z));
        }
        double[] g = LANCZOS_COEFF;
        double zm = z - 1d;
        double a = g[0];
        double t = zm + 7.5d;
        for (int i = 1; i < g.length; i++) {
            a += g[i] / (zm + i);
        }
        return Math.sqrt(2d * Math.PI) * Math.pow(t, zm + 0.5d) * Math.exp(-t) * a;
    }

    private static final double[] LANCZOS_COEFF = { 0.99999999999980993, 676.5203681218851, -1259.1392167224028,
        771.32342877765313, -176.61502916214059, 12.507343278686905, -0.13857109526572012, 9.9843695780195716e-6,
        1.5056327351493116e-7 };
}
