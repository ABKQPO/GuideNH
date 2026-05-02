package com.hfstudio.guidenh.guide.document.block.functiongraph;

/**
 * Abstract syntax tree of a parsed function expression. Nodes evaluate against two named variables:
 * {@code x} and {@code y}. Out-of-domain or arithmetic failures should produce {@link Double#NaN}
 * rather than throwing, so the renderer can simply break the polyline.
 */
public interface FunctionExpr {

    /** Evaluate this expression with the supplied variable values. */
    double evaluate(double x, double y);

    /** Constant numeric literal. */
    final class Constant implements FunctionExpr {

        private final double value;

        public Constant(double value) {
            this.value = value;
        }

        @Override
        public double evaluate(double x, double y) {
            return value;
        }
    }

    /** Variable reference. {@code which} = 0 -> x, {@code which} = 1 -> y. */
    final class Variable implements FunctionExpr {

        private final int which;

        public Variable(int which) {
            this.which = which;
        }

        @Override
        public double evaluate(double x, double y) {
            return which == 0 ? x : y;
        }
    }

    /** Unary negation. */
    final class Neg implements FunctionExpr {

        private final FunctionExpr inner;

        public Neg(FunctionExpr inner) {
            this.inner = inner;
        }

        @Override
        public double evaluate(double x, double y) {
            return -inner.evaluate(x, y);
        }
    }

    /** Absolute value, e.g. produced by {@code |expr|}. */
    final class Abs implements FunctionExpr {

        private final FunctionExpr inner;

        public Abs(FunctionExpr inner) {
            this.inner = inner;
        }

        @Override
        public double evaluate(double x, double y) {
            return Math.abs(inner.evaluate(x, y));
        }
    }

    /** Postfix factorial; uses Lanczos gamma for non-integer / negative inputs (rejects negative integers). */
    final class Factorial implements FunctionExpr {

        private final FunctionExpr inner;

        public Factorial(FunctionExpr inner) {
            this.inner = inner;
        }

        @Override
        public double evaluate(double x, double y) {
            return FunctionLibrary.factorial(inner.evaluate(x, y));
        }
    }

    /** Binary arithmetic (+ - * / % ^). */
    final class Binary implements FunctionExpr {

        public static final int ADD = 0;
        public static final int SUB = 1;
        public static final int MUL = 2;
        public static final int DIV = 3;
        public static final int MOD = 4;
        public static final int POW = 5;

        private final int op;
        private final FunctionExpr left;
        private final FunctionExpr right;

        public Binary(int op, FunctionExpr left, FunctionExpr right) {
            this.op = op;
            this.left = left;
            this.right = right;
        }

        @Override
        public double evaluate(double x, double y) {
            double a = left.evaluate(x, y);
            double b = right.evaluate(x, y);
            return switch (op) {
                case ADD -> a + b;
                case SUB -> a - b;
                case MUL -> a * b;
                case DIV -> b == 0d ? Double.NaN : a / b;
                case MOD -> b == 0d ? Double.NaN : a % b;
                case POW -> Math.pow(a, b);
                default -> Double.NaN;
            };
        }
    }

    /** Named function call with one or two arguments. */
    final class Call implements FunctionExpr {

        private final String name;
        private final FunctionExpr[] args;

        public Call(String name, FunctionExpr[] args) {
            this.name = name;
            this.args = args;
        }

        @Override
        public double evaluate(double x, double y) {
            int n = args.length;
            if (n == 1) {
                return FunctionLibrary.call1(name, args[0].evaluate(x, y));
            }
            if (n == 2) {
                return FunctionLibrary.call2(name, args[0].evaluate(x, y), args[1].evaluate(x, y));
            }
            return Double.NaN;
        }
    }
}
