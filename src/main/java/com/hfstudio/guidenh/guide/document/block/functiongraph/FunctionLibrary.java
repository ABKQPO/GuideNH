package com.hfstudio.guidenh.guide.document.block.functiongraph;

/**
 * Numeric helpers for the function graph: standard one- and two-argument numeric functions, plus a
 * Lanczos approximation of the gamma function that powers the postfix factorial operator.
 */
public class FunctionLibrary {

    protected FunctionLibrary() {}

    /** Returns true when {@code name} is recognised as a built-in function (any arity). */
    public static boolean isKnown(String name) {
        return resolveArity(name) != 0;
    }

    /** Returns 1 for unary functions, 2 for binary, otherwise 0. */
    public static int resolveArity(String name) {
        return switch (name) {
            case "sin", "cos", "tan", "asin", "acos", "atan", "sinh", "cosh", "tanh", "ln", "log", "log2", "log10", "exp", "sqrt", "cbrt", "abs", "sign", "floor", "ceil", "round", "deg", "rad", "gamma" -> 1;
            case "atan2", "min", "max", "pow", "hypot", "mod" -> 2;
            default -> 0;
        };
    }

    /** Recognised mathematical constants. Returns {@link Double#NaN} when the name is unknown. */
    public static double constant(String name) {
        return switch (name) {
            case "pi", "PI", "Pi" -> Math.PI;
            case "e", "E" -> Math.E;
            case "tau", "TAU", "Tau" -> Math.PI * 2d;
            case "phi", "PHI" -> (1d + Math.sqrt(5d)) / 2d;
            default -> Double.NaN;
        };
    }

    /** Evaluate a unary built-in. */
    public static double call1(String name, double a) {
        return switch (name) {
            case "sin" -> Math.sin(a);
            case "cos" -> Math.cos(a);
            case "tan" -> Math.tan(a);
            case "asin" -> Math.asin(a);
            case "acos" -> Math.acos(a);
            case "atan" -> Math.atan(a);
            case "sinh" -> Math.sinh(a);
            case "cosh" -> Math.cosh(a);
            case "tanh" -> Math.tanh(a);
            case "ln" -> Math.log(a);
            case "log", "log10" -> Math.log10(a);
            case "log2" -> Math.log(a) / Math.log(2d);
            case "exp" -> Math.exp(a);
            case "sqrt" -> a < 0d ? Double.NaN : Math.sqrt(a);
            case "cbrt" -> Math.cbrt(a);
            case "abs" -> Math.abs(a);
            case "sign" -> Math.signum(a);
            case "floor" -> Math.floor(a);
            case "ceil" -> Math.ceil(a);
            case "round" -> Math.rint(a);
            case "deg" -> Math.toDegrees(a);
            case "rad" -> Math.toRadians(a);
            case "gamma" -> gamma(a);
            default -> Double.NaN;
        };
    }

    /** Evaluate a binary built-in. */
    public static double call2(String name, double a, double b) {
        return switch (name) {
            case "atan2" -> Math.atan2(a, b);
            case "min" -> Math.min(a, b);
            case "max" -> Math.max(a, b);
            case "pow" -> Math.pow(a, b);
            case "hypot" -> Math.hypot(a, b);
            case "mod" -> b == 0d ? Double.NaN : a % b;
            default -> Double.NaN;
        };
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
